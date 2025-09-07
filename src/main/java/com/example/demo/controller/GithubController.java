package com.example.demo.controller;

import com.example.demo.service.*;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/DiFF/github")
public class GithubController {

    @Autowired
    private Rq rq;

    private final WebClient github;

    private final MemberService memberService;

    private final OAuthAccountService oAuthAccountService;

    private final DraftService draftService;

    private final GptService gptService;

    private final RepositoryService repositoryService;

    // util
//    private static final int MAX_PATCH_CHARS = 200_000;

    private static final String[] ALLOWED_EXTENSIONS = {
            ".mjs", ".jsx", ".java", ".ts", ".tsx", ".jsp", ".js",
            ".py", ".c", ".cs", ".cpp", ".php", ".go", ".rs",
            ".rb", ".kt", ".swift", ".xml"
    };

    private boolean isAllowedPath(String filename) {
        if (filename == null) return false;
        String f = filename.toLowerCase();

        if (f.contains("/dist/") || f.contains("/build/") || f.contains("/node_modules/")) {
            return false;
        }

        for (String ext : ALLOWED_EXTENSIONS) {
            if (f.endsWith(ext)) return true;
        }
        return false;
    }

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
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
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
        System.out.println("owner: " + owner);
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
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();
            if (res == null) {
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }

            List<Commit> commits = res.stream().map(m -> {
                Map<String, Object> commitObj = (Map<String, Object>) m.get("commit");
                Map<String, Object> authorObj = commitObj != null ? (Map<String, Object>) commitObj.get("author") : null;
                Map<String, Object> ghAuthor = (Map<String, Object>) m.get("author"); // 깃허브 계정 객체(매칭되면 존재)

                String ghLogin = ghAuthor != null ? (String) ghAuthor.get("login") : null;
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

    @GetMapping("/commit/{owner}/{repoName}/{sha}")
    public ResultData< Map<String,Object> > mkDraftByCommit(
            HttpServletRequest req,
            @PathVariable String owner,
            @PathVariable String repoName,
            @PathVariable String sha
    ) {
        System.out.println("========= mkDraftByCommit (create draft blocking) ==========");

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            // GitHub 커밋 조회
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

            if (raw == null) return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");

            // 표시/프롬프트용 최소 메타
            Map<String, Object> commitObj = (Map<String, Object>) raw.get("commit");

            // files[].patch 로 diff 조립 (바이너리/빌드 산출물 제거 + 과대 patch 샘플링)
            List<Map<String, Object>> rawFiles = (List<Map<String, Object>>) raw.getOrDefault("files", List.of());
            StringBuilder diff = new StringBuilder();
            for (Map<String, Object> f : rawFiles) {
                String filename = Objects.toString(f.get("filename"), "");
                String status = Objects.toString(f.get("status"), "");
                Object patchObj = f.get("patch");
                if (patchObj == null) continue;
                if (!isAllowedPath(filename)) continue;

                String patch = patchObj.toString();
//                patch = sampleIfTooLarge(patch); // 너무 길면 앞/뒤만 남김

                diff.append("\n\n# ").append(status).append(" ").append(filename).append("\n");
                diff.append(patch);
            }

            Long repoId = repositoryService.getRepoIdByMemberIdAndGithubRepoName(member.getId(), repoName);
            if(repoId == null){
                System.out.println("매칭된 리포 없음");
                return ResultData.from("F-500", "매칭 리포 없음");
            }

            // Draft 생성
            Draft draft = new Draft();
             draft.setRepositoryId(repoId);
            draft.setMemberId(member.getId());
            draft.setBody("");
            draft.setRegDate(LocalDateTime.now());
            draftService.saveDraft(draft);

            // GPT 호출 → 초안 본문 생성 후 업데이트
            String draftBody = gptService.makeDraft(
                    diff.toString(), draft.getRepositoryId(), draft.getMemberId(),
                    Objects.toString(raw.get("sha"), sha), draft.getId());
            draft.setBody(draftBody);
            draftService.saveDraft(draft);

            // 성공 응답: draftId
            Map<String,Object> res = Map.of("draftId", draft.getId());
            return ResultData.from("S-1", "초안 생성 성공", res);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            return ResultData.from("F-404", "커밋 또는 리포지토리를 찾을 수 없습니다.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            return ResultData.from("F-2", "초안 생성 실패: " + e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResultData<Map<String, Object>> validateRepoByUrl(
            @RequestAttribute("rq") Rq rq,
            @RequestParam("url") String url
    ) {

        System.out.println("======== validate 진입 ========");
        // 1) URL에서 owner/repo 추출
        String[] or = parseOwnerRepoFromUrl(url);
        if (or == null) {
            System.out.println("F-400 " + "유효하지 않은 GitHub 리포지토리 URL입니다. 예) https://github.com/{owner}/{repo}");
            return ResultData.from("F-400", "유효하지 않은 GitHub 리포지토리 URL입니다. 예) https://github.com/{owner}/{repo}");
        }
        String owner = or[0];
        String repoName = or[1];

        // 2) 토큰 확인
        String token = oAuthAccountService.findGithubAccessTokenByMemberId(rq.getLoginedMemberId());
        if (token == null || token.isBlank()) {
            System.out.println("F-1 " + "깃허브 연동(토큰) 없음");
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("owner", owner);
        data.put("name", repoName);
        data.put("requestedUrl", url);

        // 3) 토큰으로 조회
        try {
            Map<String, Object> authed = github.get()
                    .uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}").build(owner, repoName))
                    .headers(h -> {
                        h.setBearerAuth(token);
                        h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                        h.set("X-GitHub-Api-Version", "2022-11-28");
                        // h.set(HttpHeaders.ACCEPT, "application/vnd.github+json"); // (선택) 권장
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (authed == null) {
                System.out.println("F-2 " + "깃허브 API 응답이 비었습니다.");
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }

            // 200 OK → 존재 + 토큰으로 접근 가능
            boolean isPrivate = Boolean.TRUE.equals(authed.get("private"));

            // push 권한 확인
            Map<String, Object> perms = (Map<String, Object>) authed.get("permissions");
            boolean canPush = perms != null && Boolean.TRUE.equals(perms.get("push"));

            data.put("exists", true);
            data.put("accessibleWithToken", true);
            data.put("canPush", canPush);
            data.put("visibility", isPrivate ? "private" : "public");
            data.put("id", authed.get("id"));
            data.put("htmlUrl", authed.get("html_url"));
            data.put("defaultBranch", authed.get("default_branch"));

            if (!canPush) {
                System.out.println("F-NO-PUSH " + "푸시 권한이 없습니다.");
                return ResultData.from("F-NO-PUSH", "푸시 권한이 없습니다. 이 리포지토리는 사용할 수 없습니다.", data);
            }

            // push 가능 → 성공
            if (isPrivate) {
                System.out.println("S-1 " + "리포지토리 인증 완료(비공개, 푸시 가능) " + data);
                return ResultData.from("S-1", "리포지토리 인증 완료(비공개, 푸시 가능).", data);
            } else {
                System.out.println("S-2 " + "리포지토리 인증 완료(공개, 푸시 가능) " + data);
                return ResultData.from("S-2", "리포지토리 인증 완료(공개, 푸시 가능).", data);
            }

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            System.out.println("F-401 " + "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            // 404 → 토큰으로는 404. 공개 여부 확인(비인증 1회)
            try {
                Map<String, Object> unauth = github.get()
                        .uri(uriBuilder -> uriBuilder.path("/repos/{owner}/{repo}").build(owner, repoName))
                        .headers(h -> {
                            // 토큰 생략(=비인증 요청)
                            h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                            h.set("X-GitHub-Api-Version", "2022-11-28");
                            // h.set(HttpHeaders.ACCEPT, "application/vnd.github+json"); // (선택)
                        })
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                if (unauth != null) {
                    // 공개 리포는 존재하지만, 현재 토큰 소유자는 접근/푸시 권한이 없음 → 정책상 실패
                    data.put("exists", true);
                    data.put("accessibleWithToken", false);
                    data.put("visibility", "public");
                    data.put("id", unauth.get("id"));
                    data.put("htmlUrl", unauth.get("html_url"));
                    data.put("defaultBranch", unauth.get("default_branch"));
                    data.put("canPush", false);

                    System.out.println("F-NO-PUSH " + "공개 리포이지만 푸시 권한이 없습니다.");
                    return ResultData.from("F-NO-PUSH", "공개 리포지토리이지만 푸시 권한이 없어 사용할 수 없습니다.", data);
                }

                // unauth == null → 응답 비었음
                System.out.println("F-2 " + "깃허브 API 응답이 비었습니다.");
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");

            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e2) {
                // 비인증도 404 → 존재하지 않거나 비공개 + 권한 없음
                data.put("exists", false);
                data.put("accessibleWithToken", false);
                data.put("visibility", "unknown");
                System.out.println("F-404 " + "리포지토리가 존재하지 않거나 비공개이며 권한이 없습니다. " + data);
                return ResultData.from("F-404", "리포지토리가 존재하지 않거나 비공개이며 권한이 없습니다.", data);
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e2) {
                System.out.println("F-403 " + "비인증 호출 레이트 리밋 초과. 잠시 후 다시 시도하세요.");
                return ResultData.from("F-403", "비인증 호출 레이트 리밋 초과. 잠시 후 다시 시도하세요.");
            } catch (Exception e2) {
                System.out.println("F-2 " + "공개 여부 확인 실패: " + e2.getMessage());
                return ResultData.from("F-2", "공개 여부 확인 실패: " + e2.getMessage());
            }

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
            System.out.println("F-403 " + "접근 권한 부족 또는 레이트 리밋 초과.");
            return ResultData.from("F-403", "접근 권한 부족 또는 레이트 리밋 초과.", null);
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.out.println("F-2 " + "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            System.out.println("F-2 " + "깃허브 API 호출 실패: " + e.getMessage());
            return ResultData.from("F-2", "깃허브 API 호출 실패: " + e.getMessage());
        }
    }

    private String[] parseOwnerRepoFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String u = url.trim();

            // SSH 포맷
            if (u.startsWith("git@")) {
                int idx = u.indexOf(':');
                if (idx > 0) {
                    String path = u.substring(idx + 1);
                    if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
                    String[] parts = path.split("/");
                    if (parts.length >= 2) {
                        return new String[]{parts[0], parts[1]};
                    }
                }
                return null;
            }

            // HTTPS 포맷
            URI uri = URI.create(u);
            String host = uri.getHost();
            if (host == null || !host.toLowerCase().contains("github.com")) return null;

            String path = uri.getPath();
            if (path == null) return null;

            String[] raw = path.split("/");
            java.util.List<String> segs = new ArrayList<>();
            for (String p : raw) if (p != null && !p.isBlank()) segs.add(p);

            if (segs.size() < 2) return null;

            String owner = segs.get(0);
            String repo = segs.get(1);
            if (repo.endsWith(".git")) repo = repo.substring(0, repo.length() - 4);

            if (owner.isBlank() || repo.isBlank()) return null;
            return new String[]{owner, repo};
        } catch (Exception e) {
            return null;
        }
    }
}
