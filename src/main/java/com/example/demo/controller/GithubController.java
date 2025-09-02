package com.example.demo.controller;

import com.example.demo.service.OAuthAccountService;
import com.example.demo.service.MemberService;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/DiFF/github")
public class GithubController {

    private final WebClient github; // bean: baseUrl=https://api.github.com, ACCEPT 설정
    private final MemberService memberService;
    private final OAuthAccountService oAuthAccountService;

    @GetMapping("/repos")
    public ResultData<List<Repository>> listRepos(HttpServletRequest req) {

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        System.out.println("🐳🐳 github token: " + token);

        List<Map<String, Object>> res;

        try {
            res = github.get()
                    .uri(uri -> uri.path("/user/repos")
                            .queryParam("per_page", 100)
                            .queryParam("affiliation", "owner,collaborator,organization_member")
                            .queryParam("visibility", "all")
                            .build())
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String,Object>>>() {})
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }

        if (res == null) return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");

        if (!res.isEmpty()) {
            System.out.println("sample keys: " + res.get(0).keySet());
            System.out.println("full_name: " + res.get(0).get("full_name"));
            System.out.println("html_url: " + res.get(0).get("html_url"));
        }

        List<Repository> repos = res.stream().map(m -> {
            Repository r = new Repository();

            Object ghIdObj = m.get("id");
            if (ghIdObj instanceof Number) {
                r.setId(((Number) ghIdObj).longValue());
            }

            r.setName((String) m.get("name"));
            r.setUrl((String) m.get("html_url"));
            r.setAPrivate(Boolean.TRUE.equals(m.get("private")));
            r.setDefaultBranch((String) m.get("default_branch"));
            Object login = ((Map<?, ?>) m.get("owner")).get("login");
            r.setOwner(login != null ? login.toString() : null);
            return r;
        }).toList();

        return ResultData.from("S-1", "리포지토리 조회 성공", repos);
    }

    @GetMapping("/commits")
    public ResultData<List<Commit>> getCommitList(
            HttpServletRequest req,
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "50") int perPage
    ) {
        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            List<Map<String, Object>> res = github.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path("/repos/{owner}/{repo}/commits")
                                .queryParam("per_page", Math.min(Math.max(perPage, 1), 100))
                                .queryParam("page", Math.max(page, 1))
                                .build(owner, repo);
                        return uriBuilder.path("")
                                .queryParamIfPresent("sha", java.util.Optional.ofNullable(branch))
                                .build(owner, repo); // owner/repo는 이미 위에서 바인딩됨
                    })
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .block();

            if (res == null) {
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }

            // GitHub 응답 → 내부 VO 매핑
            List<Commit> commits = res.stream().map(m -> {
                Commit c = new Commit();
                c.setSha((String) m.get("sha"));
                c.setHtmlUrl((String) m.get("html_url"));

                Map<String, Object> commitObj = (Map<String, Object>) m.get("commit");
                if (commitObj != null) {
                    c.setMessage((String) commitObj.getOrDefault("message", ""));

                    Map<String, Object> authorObj = (Map<String, Object>) commitObj.get("author");
                    if (authorObj != null) {
                        Object name = authorObj.get("name");
                        Object date = authorObj.get("date");
                        if (name != null) c.setAuthorName(name.toString());
                        if (date != null) c.setAuthoredAt(date.toString());
                    }
                }

                // parents → 가장 가까운 부모 SHA (있으면)
                List<Map<String, Object>> parents = (List<Map<String, Object>>) m.get("parents");
                if (parents != null && !parents.isEmpty()) {
                    Object psha = parents.get(0).get("sha");
                    if (psha != null) c.setParentSha(psha.toString());
                }

                // author(깃허브 계정) 정보가 별도로 있을 수 있음
                Map<String, Object> ghAuthor = (Map<String, Object>) m.get("author");
                if (ghAuthor != null) {
                    Object login = ghAuthor.get("login");
                    if (login != null) c.setAuthorLogin(login.toString());
                }
                return c;
            }).toList();

            return ResultData.from("S-1", "커밋 조회 성공", commits);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            return ResultData.from("F-404", "리포지토리를 찾을 수 없습니다. owner/repo를 확인하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
            return ResultData.from("F-403", "접근 권한 부족 또는 레이트리밋 초과.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }
    }

    @GetMapping("/commits/{owner}/{repo}/{sha}")
    public ResultData<Map<String, Object>> getCommitDetail(
            HttpServletRequest req,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String sha
    ) {
        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            Map<String, Object> commit = github.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/commits/{sha}")
                            .build(owner, repo, sha))
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (commit == null) {
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }

            // commit.get("files") 안에 filename, status, additions, deletions, patch 등이 포함됨
            // patch는 unified diff(텍스트 파일)이고, 큰 바이너리 파일은 patch가 없을 수 있음
            return ResultData.from("S-1", "커밋 상세 조회 성공", commit);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            return ResultData.from("F-404", "커밋 또는 리포지토리를 찾을 수 없습니다.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }
    }

}
