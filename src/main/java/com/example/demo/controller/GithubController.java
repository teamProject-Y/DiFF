package com.example.demo.controller;

import com.example.demo.service.OAuthAccountService;
import com.example.demo.service.MemberService;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/DiFF/github")
public class GithubController {

    @Autowired
    private Rq rq;

    private final WebClient github;

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
            System.out.println("name: " + res.get(0).get("name"));
            System.out.println("sample keys: " + res.get(0).keySet());
            System.out.println("owner: " + res.get(0).get("owner"));
            System.out.println("html_url: " + res.get(0).get("html_url"));
        }

        List<Repository> repos = res.stream().map(m -> {
            Repository r = new Repository();

            Object ghIdObj = m.get("id");
            if (ghIdObj instanceof Number) {
                r.setId(((Number) ghIdObj).longValue());
            }

            r.setName((String) m.get("name"));
            r.setGithubName((String) m.get("name"));
            r.setUrl((String) m.get("html_url"));
            r.setAPrivate(Boolean.TRUE.equals(m.get("private")));
            r.setDefaultBranch((String) m.get("default_branch"));
            r.setGithubOwner((String) ((Map<?, ?>) m.get("owner")).get("login"));
            Object login = ((Map<?, ?>) m.get("owner")).get("login");
            r.setOwner(login != null ? login.toString() : null);

            System.out.println("githubOwner: " + r.getGithubOwner());
            System.out.println("githubName: " + r.getGithubName());
            return r;
        }).toList();

        return ResultData.from("S-1", "리포지토리 조회 성공", repos);
    }

    @GetMapping("/commits")
    public ResultData<List<Commit>> getCommitList(
            @RequestAttribute("rq") Rq rq,
            HttpServletRequest req,
            @RequestParam String owner,
            @RequestParam String repoName,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int perPage
    ) {

        System.out.println("rq memberId = " + rq.getLoginedMemberId());
        System.out.println("owner: "  + owner);
        System.out.println("repoName: " + repoName);
        System.out.println("page: " + page);
        System.out.println("perPage: " + perPage);

        Rq raq = (Rq) req.getAttribute("rq");
        System.out.println("raq memberId = " + raq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(rq.getLoginedMemberId());

        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            List<Map<String, Object>> res = github.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/commits")
                            .queryParam("per_page", perPage)
                            .queryParam("page", page)
                            .queryParamIfPresent("sha", java.util.Optional.ofNullable(branch))
                            .build(owner, repoName)
                    )
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

            List<Commit> commits = res.stream().map(m -> {
                Map<String, Object> commitObj  = (Map<String, Object>) m.get("commit");
                Map<String, Object> authorObj  = commitObj != null ? (Map<String, Object>) commitObj.get("author") : null;
                Map<String, Object> ghAuthor   = (Map<String, Object>) m.get("author"); // 깃허브 계정 객체(매칭되면 존재)

                String ghLogin  = ghAuthor != null ? (String) ghAuthor.get("login") : null;
                String ghAvatar = ghAuthor != null ? (String) ghAuthor.get("avatar_url") : null;

                String authorNameFromMeta = authorObj != null ? (String) authorObj.get("name") : null;
                String finalAuthorName = (ghLogin != null && !ghLogin.isBlank())
                        ? ghLogin
                        : (authorNameFromMeta != null && !authorNameFromMeta.isBlank() ? authorNameFromMeta : "unknown");

                String authoredAt = authorObj != null ? (String) authorObj.get("date") : null;

                List<Map<String, Object>> parents = (List<Map<String, Object>>) m.get("parents");
                String parentSha = (parents != null && !parents.isEmpty() && parents.get(0).get("sha") != null)
                        ? parents.get(0).get("sha").toString()
                        : null;

                return Commit.builder()
                        .sha((String) m.get("sha"))
                        .htmlUrl((String) m.get("html_url"))
                        .message(commitObj != null ? (String) commitObj.getOrDefault("message", "") : "")

                        .AuthorLogin(ghLogin)
                        .AuthorAvatarUrl(ghAvatar)
                        .AuthorName(finalAuthorName)
                        .AuthoredAt(authoredAt)

                        .parentSha(parentSha)
                        .build();
            }).toList();

            return ResultData.from("S-1", "커밋 조회 성공", "commits", commits);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            System.out.println("github error: " + e.getMessage());
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            System.out.println("github error: " + e.getMessage());
            return ResultData.from("F-404", "No search results found.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
            System.out.println("github error: " + e.getMessage());
            return ResultData.from("F-403", "접근 권한 부족 또는 레이트리밋 초과.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.out.println("github error: " + e.getMessage());
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            System.out.println("github error: " + e.getMessage());
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }
    }

    @GetMapping("/commits/{owner}/{repo}/{sha}")
    public ResultData<Commit> getCommitDetail(
            HttpServletRequest req,
            @PathVariable String owner,
            @PathVariable String repoName,
            @PathVariable String sha
    ) {
        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            Map<String, Object> raw = github.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/commits/{sha}")
                            .build(owner, repoName, sha))
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (raw == null) {
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }

            Map<String, Object> commitObj = (Map<String, Object>) raw.get("commit");
            Map<String, Object> authorMeta = commitObj != null ? (Map<String, Object>) commitObj.get("author") : null; // name/date
            Map<String, Object> ghAuthor   = (Map<String, Object>) raw.get("author"); // login/avatar_url (깃허브 계정)

            String shaVal = (String) raw.get("sha");
            String htmlUrl = (String) raw.get("html_url");
            String message = commitObj != null ? (String) commitObj.get("message") : null;

            String authorLogin = ghAuthor != null ? (String) ghAuthor.get("login") : null;
            String authorAvatarUrl = ghAuthor != null ? (String) ghAuthor.get("avatar_url") : null;
            String authorNameFromMeta = authorMeta != null ? (String) authorMeta.get("name") : null;
            String authoredAt = authorMeta != null ? (String) authorMeta.get("date") : null;

            String authorName = (authorLogin != null && !authorLogin.isBlank())
                    ? authorLogin
                    : (authorNameFromMeta != null && !authorNameFromMeta.isBlank() ? authorNameFromMeta : "unknown");

            String parentSha = null;
            List<Map<String, Object>> parents = (List<Map<String, Object>>) raw.get("parents");
            if (parents != null && !parents.isEmpty()) {
                Object psha = parents.get(0).get("sha");
                if (psha != null) parentSha = psha.toString();
            }

            Map<String, Object> rawStats = (Map<String, Object>) raw.get("stats");
            Map<String, Object> stats = null;
            if (rawStats != null) {
                stats = new java.util.LinkedHashMap<>();
                if (rawStats.get("additions") != null) stats.put("additions", rawStats.get("additions"));
                if (rawStats.get("deletions") != null) stats.put("deletions", rawStats.get("deletions"));
                if (rawStats.get("total") != null)     stats.put("total",     rawStats.get("total"));
            }

            List<Map<String, Object>> rawFiles = (List<Map<String, Object>>) raw.get("files");
            List<Map<String, Object>> files = null;
            if (rawFiles != null) {
                files = new java.util.ArrayList<>(rawFiles.size());
                for (Map<String, Object> f : rawFiles) {
                    java.util.Map<String, Object> slim = new java.util.LinkedHashMap<>();
                    Object filename  = f.get("filename");
                    Object status    = f.get("status");
                    Object additions = f.get("additions");
                    Object deletions = f.get("deletions");
                    Object patch     = f.get("patch");

                    if (filename != null)  slim.put("filename",  filename);
                    if (status != null)    slim.put("status",    status);
                    if (additions != null) slim.put("additions", additions);
                    if (deletions != null) slim.put("deletions", deletions);
                    if (patch != null)     slim.put("patch",     patch);

                    files.add(slim);
                }
            }

            Commit detail = Commit.builder()
                    .sha(shaVal)
                    .message(message)
                    .htmlUrl(htmlUrl)
                    .parentSha(parentSha)
                    .AuthorName(authorName)
                    .AuthoredAt(authoredAt)
                    .AuthorAvatarUrl(authorAvatarUrl)
                    .AuthorLogin(authorLogin)
                    .stats(stats)
                    .files(files)
                    .build();

            return ResultData.from("S-1", "커밋 상세 조회 성공", detail);

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
