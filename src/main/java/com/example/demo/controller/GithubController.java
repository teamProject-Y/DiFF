package com.example.demo.controller;

import com.example.demo.service.*;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

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

    private final NotificationService notificationService;

    private final FcmService fcmService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GithubController.class);

    // 요청 단위 스텝 로그 누적용
    private static final class StepLog {
        final String reqId;
        final java.util.List<String> steps = new java.util.ArrayList<>();
        StepLog(String reqId) { this.reqId = reqId; }
        void add(String msg) {
            String line = "[" + reqId + "] " + msg;
            steps.add(line);
            log.info(line);              // SLF4J로 즉시 출력(버퍼링 이슈 회피)
        }
        void dumpSummary() {
            log.info("[{}] ----- SUMMARY START -----", reqId);
            for (String s : steps) log.info("{}", s);
            log.info("[{}] ----- SUMMARY END -----", reqId);
        }
    }
    private StepLog newStepLog() {
        return new StepLog(java.util.UUID.randomUUID().toString().substring(0,8));
    }


    // util
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
            System.out.println("github error 401: " + e.getMessage());
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            System.out.println("github error 404: " + e.getMessage());
            return ResultData.from("F-404", "No search results found.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Forbidden e) {
            System.out.println("github error 403: " + e.getMessage());
            return ResultData.from("F-403", "접근 권한 부족 또는 레이트리밋 초과.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.out.println("github error 2: " + e.getMessage());
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("github error 2-2: " + e.getMessage());
            return ResultData.from("F-3", "깃허브 API 호출 실패: " + e.getMessage());
        }
    }

    @GetMapping("/commit/{repoId}/{owner}/{repoName}/{sha}")
    public ResultData<Map<String,Object>> mkDraftByCommit(
            HttpServletRequest req,
            @PathVariable Long repoId,
            @PathVariable String owner,
            @PathVariable String repoName,
            @PathVariable String sha
    ) {
        System.out.println("========= mkDraftByCommit (create draft blocking) ==========");
        StepLog step = newStepLog(); // SLF4J + 요약 로그
        long tStart = System.nanoTime();

        step.add("req owner=" + owner + ", repo=" + repoName + ", sha=" + sha);

        Rq rq = (Rq) req.getAttribute("rq");
        Member member = memberService.getMemberById((long) rq.getLoginedMemberId());

        String token = oAuthAccountService.findGithubAccessTokenByMemberId(member.getId());
        if (token == null || token.isBlank()) {
            step.add("❌ 토큰 없음");
            step.dumpSummary();
            return ResultData.from("F-1", "깃허브 연동(토큰) 없음");
        }

        try {
            // 1) 커밋 조회
            long t1 = System.nanoTime();
            step.add("✅ 1. 커밋 조회 시작");
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
                step.add("❌ 2. 깃허브 응답 없음");
                step.dumpSummary();
                return ResultData.from("F-2", "깃허브 API 응답이 비었습니다.");
            }
            step.add(String.format("✅ 2. 깃허브 응답 있음 (%.1f ms)", (System.nanoTime() - t1)/1e6));

            // 2) diff 조립(허용 파일만)
            List<Map<String, Object>> rawFiles = (List<Map<String, Object>>) raw.getOrDefault("files", List.of());
            StringBuilder diff = new StringBuilder();
            int patchedFiles = 0;
            for (Map<String, Object> f : rawFiles) {
                String filename = Objects.toString(f.get("filename"), "");
                String status = Objects.toString(f.get("status"), "");
                Object patchObj = f.get("patch");
                if (patchObj == null) continue;
                if (!isAllowedPath(filename)) continue;
                patchedFiles++;
                diff.append("\n\n# ").append(status).append(" ").append(filename).append("\n");
                diff.append(patchObj.toString());
            }
            step.add("ℹ️ diff 조립: files=" + rawFiles.size() + ", patchedFiles(allowed)=" + patchedFiles + ", diffLen=" + diff.length());

            // 3) 리포 매칭
//            Long repoId = repositoryService.getRepoIdByMemberIdAndGithubRepoName(member.getId(), repoName);
            if (repoId == null) {
                step.add("❌ 3. 매칭된 리포 없음 (memberId=" + member.getId() + ", repoName=" + repoName + ")");
                step.dumpSummary();
                return ResultData.from("F-500", "매칭 리포 없음");
            }
            step.add("✅ 3. 리포 매칭 됨 repoId=" + repoId);

            // 4) Draft 생성
            Draft draft = new Draft();
            draft.setRepositoryId(repoId);
            draft.setMemberId(member.getId());
            draft.setBody("");
            draft.setRegDate(LocalDateTime.now());
            draft.setChecksum(sha);
            draftService.saveDraft(draft);
            step.add("✅ 4. draft 기본값 생성 draftId=" + draft.getId());

            // 5) ZIP 다운로드 → 빌드 ZIP 생성 → 업로드
            Path zipPath = null;
            Path builtZip = null;
            boolean downloadOk = false, buildOk = false, uploadOk = false;

            try {
                step.add("✴️ 5-1. zipball 다운로드 시작");
                long td = System.nanoTime();
                zipPath = downloadZipballToTempFile(owner, repoName, sha, token);
                if (zipPath != null && Files.exists(zipPath)) {
                    long size = 0L;
                    try { size = Files.size(zipPath); } catch (Exception ignore) {}
                    step.add(String.format("✅ zip 다운로드 완료: %s (%d bytes, %.1f ms)", zipPath, size, (System.nanoTime() - td)/1e6));
                    downloadOk = true;
                } else {
                    step.add("❌ zip 다운로드 실패: zipPath is null or not exists");
                }

                if (downloadOk) {
                    step.add("✴️ 5-2. 서버측 빌드 수행(컨테이너) → 산출물 포함 ZIP 생성 시작");
                    long tb = System.nanoTime();
                    builtZip = buildZipForUpload(zipPath, repoId, sha);
                    if (builtZip != null && Files.exists(builtZip)) {
                        long bsize = 0L;
                        try { bsize = Files.size(builtZip); } catch (Exception ignore) {}
                        step.add(String.format("✅ 빌드 ZIP 생성 완료: %s (%d bytes, %.1f ms)", builtZip, bsize, (System.nanoTime() - tb)/1e6));
                        buildOk = true;
                    } else {
                        step.add("❌ 빌드 ZIP 생성 실패: builtZip is null or not exists");
                    }
                }

                if (buildOk) {
                    step.add("✴️ 5-3. /upload 업로드 시작 (builtZip)");
                    long tu = System.nanoTime();
                    Map<String, Object> meta = new LinkedHashMap<>();
                    meta.put("memberId", member.getId());
                    meta.put("repositoryId", repoId);
                    meta.put("draftId", draft.getId());
                    meta.put("diffId", 0L);
                    meta.put("lastChecksum", sha);

                    postZipFileToUpload(builtZip, meta);
                    uploadOk = true;
                    step.add(String.format("✅ 업로드 완료 (%.1f ms)", (System.nanoTime() - tu)/1e6));
                }
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException we) {
                // zipball 3xx/4xx/5xx 같은 경우
                step.add("❌ zip/build/upload 중 WebClient 오류: " + we.getStatusCode().value() + " " + we.getStatusText());
                log.error("zip/build/upload WebClient error", we);
            } catch (Exception zerr) {
                step.add("❌ zip/build/upload 중 예외: " + zerr.getMessage());
                log.error("zip/build/upload exception", zerr);
            } finally {
                // 실제 상태에 맞는 정리 로그
                if (zipPath != null) {
                    try {
                        long s = Files.exists(zipPath) ? Files.size(zipPath) : -1L;
                        step.add("🧹 zipPath 정리: " + zipPath + " (exists=" + Files.exists(zipPath) + ", size=" + s + ")");
                        Files.deleteIfExists(zipPath);
                    } catch (Exception e) {
                        step.add("⚠️ zipPath 삭제 실패: " + e.getMessage());
                        log.warn("delete zipPath failed", e);
                    }
                } else {
                    step.add("ℹ️ zipPath == null (다운로드 실패 가능)");
                }
                if (builtZip != null) {
                    try {
                        long s = Files.exists(builtZip) ? Files.size(builtZip) : -1L;
                        step.add("🧹 builtZip 정리: " + builtZip + " (exists=" + Files.exists(builtZip) + ", size=" + s + ")");
                        Files.deleteIfExists(builtZip);
                    } catch (Exception e) {
                        step.add("⚠️ builtZip 삭제 실패: " + e.getMessage());
                        log.warn("delete builtZip failed", e);
                    }
                } else {
                    step.add("ℹ️ builtZip == null (빌드 실패 가능)");
                }
            }

            step.add("✅ 5. zip/build/upload 종료: downloadOk=" + downloadOk + ", buildOk=" + buildOk + ", uploadOk=" + uploadOk);

            // 6) GPT 초안 생성/저장
            long tg = System.nanoTime();
            String draftBody = gptService.makeDraft(
                    diff.toString(),
                    draft.getRepositoryId(),
                    draft.getMemberId(),
                    Objects.toString(raw.get("sha"), sha),
                    draft.getId());
            draft.setBody(draftBody);
            draftService.saveDraft(draft);
            step.add(String.format("✅ 6. 초안 저장 성공 draftId=%d (%.1f ms)", draft.getId(), (System.nanoTime() - tg)/1e6));

            // 7) 알림 + FCM
            if (member != null) {
                String message = "Your draft has been created.";
                Notification notification = Notification.builder()
                        .memberId(member.getId())
                        .type("DRAFT")
                        .message(message)
                        .isRead(false)
                        .relId(draft.getId())
                        .build();
                notificationService.saveNotification(notification);
                step.add("✅ 7. 알림 저장 성공");

                if (member.isAllowDraftNotification()) {
                    if (member.getFcmToken() != null && !member.getFcmToken().isEmpty()) {
                        fcmService.sendMessage(member.getFcmToken(), "Your draft has been created.", message, null);
                        step.add("✅ 8. FCM 발송 성공");
                    } else {
                        step.add("⚠️ FCM 토큰 없음 → 푸시 스킵");
                    }
                } else {
                    step.add("⚠️ Draft 알림 OFF → 푸시 스킵");
                }
            }

            Map<String,Object> res = Map.of("draftId", draft.getId());
            step.add(String.format("✅ 9. 초안 생성 완료 (총 %.1f ms)", (System.nanoTime() - tStart)/1e6));
            step.dumpSummary();
            return ResultData.from("S-1", "초안 생성 성공", res);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized e) {
            step.add("❌ 401 Unauthorized");
            log.error("GitHub 401", e);
            step.dumpSummary();
            return ResultData.from("F-401", "깃허브 인증 실패(토큰 만료/폐기). 다시 연동하세요.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException.NotFound e) {
            step.add("❌ 404 Not Found");
            log.error("GitHub 404", e);
            step.dumpSummary();
            return ResultData.from("F-404", "커밋 또는 리포지토리를 찾을 수 없습니다.");
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            step.add("❌ WebClient error: " + e.getStatusCode().value() + " " + e.getStatusText());
            log.error("GitHub WebClient error", e);
            step.dumpSummary();
            return ResultData.from("F-2", "깃허브 API 오류: " + e.getStatusCode().value() + " " + e.getStatusText());
        } catch (Exception e) {
            step.add("❌ general error: " + e.getMessage());
            log.error("mkDraftByCommit general error", e);
            step.dumpSummary();
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
                        h.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
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
//            data.put("htmlUrl", authed.get("html_url"));
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
                             h.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                        })
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .block();

                if (unauth != null) {
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

    // ====== 스트리밍: zipball을 임시 파일에 저장 (리다이렉트 포함) ======
    private Path downloadZipballToTempFile(String owner, String repo, String ref, String token) throws Exception {
        Path tmp = Files.createTempFile("zipball-", ".zip");

        Mono<Path> m = github.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/zipball/{ref}")
                        .build(owner, repo, ref))
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set(HttpHeaders.USER_AGENT, "DiFF-App/1.0");
                    h.set("X-GitHub-Api-Version", "2022-11-28");
                    // 1차 호출은 JSON 리다이렉트만 받는다
                    h.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                })
                .exchangeToMono(r -> {
                    if (r.statusCode().is3xxRedirection()) {
                        String loc = r.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
                        if (loc == null) return r.createException().flatMap(Mono::error);

                        // 2차: 실제 ZIP 다운로드 (codeload) - octet-stream으로 스트리밍 저장
                        return WebClient.builder()
                                .defaultHeader(HttpHeaders.USER_AGENT, "DiFF-App/1.0")
                                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token) // private repo 대비
                                .build()
                                .get()
                                .uri(loc)
                                .accept(MediaType.APPLICATION_OCTET_STREAM)
                                .retrieve()
                                .bodyToFlux(DataBuffer.class)
                                .transform(dbFlux -> DataBufferUtils.write(dbFlux, tmp,
                                        StandardOpenOption.TRUNCATE_EXISTING))
                                .then(Mono.just(tmp));
                    } else if (r.statusCode().is2xxSuccessful()) {
                        // 드물게 본문을 직접 줄 수도 있음 → 그대로 스트리밍
                        Flux<DataBuffer> body = r.bodyToFlux(DataBuffer.class);
                        return DataBufferUtils.write(body, tmp, StandardOpenOption.TRUNCATE_EXISTING)
                                .then(Mono.just(tmp));
                    } else {
                        return r.createException().flatMap(Mono::error);
                    }
                })
                // 415 Unsupported Media Type이면 codeload 직접 호출로 폴백
                .onErrorResume(
                        org.springframework.web.reactive.function.client.WebClientResponseException.UnsupportedMediaType.class,
                        e -> {
                            String codeload = String.format(
                                    "https://codeload.github.com/%s/%s/legacy.zip/%s", owner, repo, ref);
                            return WebClient.builder()
                                    .defaultHeader(HttpHeaders.USER_AGENT, "DiFF-App/1.0")
                                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                    .build()
                                    .get()
                                    .uri(codeload)
                                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                                    .retrieve()
                                    .bodyToFlux(DataBuffer.class)
                                    .transform(dbFlux -> DataBufferUtils.write(dbFlux, tmp,
                                            StandardOpenOption.TRUNCATE_EXISTING))
                                    .then(Mono.just(tmp));
                        }
                );

        Path saved = m.block();
        if (saved == null || Files.size(saved) == 0) {
            Files.deleteIfExists(tmp);
            throw new IllegalStateException("zipball 다운로드 실패 또는 빈 파일");
        }
        return saved;
    }

    // ====== /upload 로 파일 멀티파트 전송 ======
    private void postZipFileToUpload(Path zipPath, Map<String, Object> meta) throws Exception {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new FileSystemResource(zipPath));
        parts.add("meta", new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(meta));

        String resp = WebClient.create("http://localhost:8080")
                .post()
                .uri("/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .bodyValue(parts)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("upload response: " + resp);
    }

    // ====== (A) ZIP → 언팩 → Docker 빌드 → 산출물 포함 새 ZIP 생성 ======
    private Path buildZipForUpload(Path inputZip, Long repositoryId, String sha) throws Exception {
        final String dockerBin = Optional.ofNullable(System.getenv("DOCKER_BIN")).orElse("docker");
        final long timeoutSeconds = Optional.ofNullable(System.getenv("BUILD_TIMEOUT_SECONDS"))
                .map(Long::parseLong).orElse(900L);

        // 1) 워크스페이스
        Path workspace = Files.createTempDirectory("diff-ws-" + repositoryId + "-" + sha + "-");
        Path srcDir = workspace.resolve("src");
        unzipGitHubZip(inputZip, srcDir); // GitHub zipball 최상위 디렉토리 제거하여 전개

        // 2) 빌드 타입 감지 → 플랜 구성 → Docker 빌드/테스트/커버리지(옵션)
        BuildType bt = detectBuildType(srcDir);
        BuildPlan plan = makeBuildPlanForBuildOnly(bt, srcDir);
        ExecResult buildRes = runDocker(dockerBin, plan.image, srcDir, plan.env, plan.commands, plan.volumes, timeoutSeconds);

        // log
        System.out.println("[build] exit=" + buildRes.exitCode);
        System.out.println("[build] tail:\n" + tail(buildRes.log, 1500));

        Path[] evidences = {
                srcDir.resolve("target/classes"),
                srcDir.resolve("target/site/jacoco/jacoco.xml"),
                srcDir.resolve("build/classes"),
                srcDir.resolve("build/reports/jacoco/test/jacocoTestReport.xml"),
                srcDir.resolve("coverage/lcov.info"),
                srcDir.resolve("dist"),
        };
        for (Path p : evidences) {
            try {
                if (Files.exists(p)) {
                    long sz = Files.isDirectory(p) ? -1L : Files.size(p);
                    System.out.println("[build] artifact found: " + p + " size=" + sz);
                }
            } catch (Exception ignore) {}
        }

        if (buildRes.exitCode != 0) {
            throw new IllegalStateException("Build failed (type=" + bt + "):\n" + tail(buildRes.log, 4000));
        }

        // 3) 빌드 산출물 포함 새 ZIP 만들기 (소스 + target/build + coverage 등, 캐시는 제외)
        Path builtZip = workspace.resolve("built-" + repositoryId + "-" + sha + ".zip");
        zipDirectorySelective(srcDir, builtZip);
        return builtZip; // 호출부에서 업로드 후 삭제 권장
    }

    // ====== (B) ZIP 언팩 (GitHub zipball 최상위 디렉토리 strip) ======
    private void unzipGitHubZip(Path zip, Path dest) throws IOException {
        Files.createDirectories(dest);
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
            java.util.zip.ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String normalized = stripFirstDir(e.getName());
                if (normalized.isBlank()) continue;
                Path out = dest.resolve(normalized).normalize();
                Files.createDirectories(out.getParent());
                Files.copy(zis, out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    private String stripFirstDir(String name) {
        int idx = name.indexOf('/');
        return (idx >= 0 && idx + 1 < name.length()) ? name.substring(idx + 1) : name;
    }

    // ====== (C) 빌드 타입 감지 ======
    private enum BuildType { MAVEN, GRADLE, NODE, PYTHON, GO, UNKNOWN }
    private BuildType detectBuildType(Path src) {
        if (Files.exists(src.resolve("pom.xml"))) return BuildType.MAVEN;
        if (Files.exists(src.resolve("build.gradle")) || Files.exists(src.resolve("build.gradle.kts"))) return BuildType.GRADLE;
        if (Files.exists(src.resolve("package.json"))) return BuildType.NODE;
        if (Files.exists(src.resolve("pyproject.toml")) || Files.exists(src.resolve("requirements.txt"))) return BuildType.PYTHON;
        if (Files.exists(src.resolve("go.mod"))) return BuildType.GO;
        return BuildType.UNKNOWN;
    }

    // ====== (D) 빌드 전용 플랜 (Sonar 없음, 테스트/커버리지는 best-effort) ======
    private static class BuildPlan {
        String image;
        java.util.List<String> commands;
        java.util.Map<String, String> env;
        java.util.List<String> volumes;
        BuildPlan(String image, java.util.List<String> commands, java.util.Map<String, String> env, java.util.List<String> volumes) {
            this.image = image; this.commands = commands; this.env = env; this.volumes = volumes;
        }
    }

    // ====== (D) 빌드 전용 플랜 (테스트는 선택: 실패해도 빌드는 성공 처리) ======
    private BuildPlan makeBuildPlanForBuildOnly(BuildType t, Path srcDir) {
        switch (t) {
            case MAVEN -> {
                // 1) package 는 테스트 완전 스킵(빌드 산출물 보장)
                // 2) 테스트는 별도로 best-effort 수행하고 실패해도 무시(|| true)
                // 3) jacoco 리포트도 best-effort
                return new BuildPlan(
                        "maven:3.9-eclipse-temurin-17",
                        java.util.List.of(
                                "mvn -B -q -DskipTests=true -DskipITs package",
                                "mvn -B -q -DskipTests=false -Dsurefire.skipAfterFailureCount=1 -Dsurefire.forkCount=1 -Dsurefire.reuseForks=true test || true",
                                "mvn -B -q jacoco:report || true"
                        ),
                        // 테스트 포크 안정화 & 메모리 절약
                        java.util.Map.of(
                                "MAVEN_OPTS", "-Xmx768m -XX:+UseSerialGC -Djava.awt.headless=true"
                        ),
                        java.util.List.of("diff-m2:/root/.m2")
                );
            }
            case GRADLE -> {
                boolean hasWrapper = Files.exists(srcDir.resolve("gradlew"));
                String gradleCmd = hasWrapper ? "chmod +x ./gradlew && ./gradlew" : "gradle";
                return new BuildPlan(
                        "gradle:8.9-jdk17",
                        java.util.List.of(
                                gradleCmd + " assemble --no-daemon --console=plain",
                                gradleCmd + " test --no-daemon --console=plain || true",
                                gradleCmd + " jacocoTestReport --no-daemon --console=plain || true"
                        ),
                        java.util.Map.of(
                                "GRADLE_OPTS", "-Xmx768m -Djava.awt.headless=true"
                        ),
                        java.util.List.of("diff-gradle:/home/gradle/.gradle")
                );
            }
            case NODE -> {
                String installer =
                        Files.exists(srcDir.resolve("pnpm-lock.yaml")) ? "corepack enable && pnpm i -f" :
                                Files.exists(srcDir.resolve("yarn.lock"))      ? "corepack enable && yarn --frozen-lockfile" :
                                        "npm ci";
                return new BuildPlan(
                        "node:20",
                        java.util.List.of(
                                installer,
                                "npm run build || true",          // 빌드 우선
                                "npm test -- --coverage || true"  // 테스트는 선택
                        ),
                        java.util.Map.of("CI","true"),
                        java.util.List.of("diff-npm:/root/.npm")
                );
            }
            case PYTHON -> {
                return new BuildPlan(
                        "python:3.11",
                        java.util.List.of(
                                "[ -f requirements.txt ] && pip install -r requirements.txt || true",
                                "pip install pytest coverage build || true",
                                "python -m build || true",
                                "pytest --maxfail=1 --disable-warnings --cov=. --cov-report=xml || true"
                        ),
                        java.util.Map.of(),
                        java.util.List.of("diff-pip:/root/.cache/pip")
                );
            }
            case GO -> {
                return new BuildPlan(
                        "golang:1.22",
                        java.util.List.of(
                                "go mod download",
                                "go build ./... || true",
                                "go test ./... -coverprofile=coverage.out || true"
                        ),
                        java.util.Map.of(),
                        java.util.List.of("diff-go:/go/pkg/mod")
                );
            }
            default -> {
                return new BuildPlan(
                        "alpine:3.20",
                        java.util.List.of("echo 'No build tool detected'"),
                        java.util.Map.of(),
                        java.util.List.of()
                );
            }
        }
    }

    // ====== (E) Docker 실행기 ======
    private static class ExecResult { int exitCode; String log; ExecResult(int c, String l){exitCode=c;log=l;} }
    private ExecResult runDocker(String dockerBin, String image, Path workDir, java.util.Map<String,String> env,
                                 java.util.List<String> commands, java.util.List<String> volumes, long timeoutSeconds) throws Exception {
        String joined = String.join(" && ", commands);

        java.util.List<String> cmd = new java.util.ArrayList<>(java.util.List.of(
                dockerBin, "run", "--rm",
                "-v", workDir.toAbsolutePath() + ":/work",
                "-w", "/work"
        ));
        for (String v : volumes) { cmd.addAll(java.util.List.of("-v", v)); }
        env.forEach((k,v) -> { cmd.add("-e"); cmd.add(k + "=" + v); });
        cmd.add(image);
        cmd.add("bash"); cmd.add("-lc"); cmd.add(joined);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (var br = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            var es = java.util.concurrent.Executors.newSingleThreadExecutor();
            var copy = es.submit(() -> br.lines().forEach(l -> out.append(l).append('\n')));
            boolean finished = p.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                copy.cancel(true);
                es.shutdownNow();
                return new ExecResult(124, "Timeout after " + timeoutSeconds + "s\n" + out);
            }
            copy.get(2, java.util.concurrent.TimeUnit.SECONDS);
            es.shutdown();
        }
        return new ExecResult(p.exitValue(), out.toString());
    }

    // ====== (F) 빌드 산출물 포함 ZIP 생성 (캐시/의존성 디렉토리는 제외) ======
    private void zipDirectorySelective(Path root, Path zipOut) throws IOException {
        java.util.Set<String> EXCLUDES = java.util.Set.of(
                ".git/", ".idea/", ".vscode/", "node_modules/", ".gradle/", "build-cache/",
                "target/surefire-reports/temp/", "__pycache__/", ".m2/", ".venv/", "out/", "bin/"
        );
        // 포함 우선 디렉토리 힌트(없어도 되지만 우선 포함되도록)
        java.util.Set<String> INCLUDE_HINTS = java.util.Set.of(
                "target/", "build/", "coverage/", "build/reports/", "dist/"
        );

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipOut))) {
            final int rootLen = root.toAbsolutePath().toString().length() + 1;
            java.nio.file.Files.walk(root)
                    .filter(Files::isRegularFile)
                    .forEach(p -> {
                        String rel = p.toAbsolutePath().toString().substring(rootLen).replace("\\","/");
                        // 제외 규칙
                        for (String ex : EXCLUDES) {
                            if (rel.startsWith(ex) || rel.contains("/" + ex)) return;
                        }
                        // 너무 큰 산출물은 제외(예: 50MB 이상) — 필요시 조정
                        try {
                            long size = Files.size(p);
                            if (size > 50L * 1024 * 1024) return;
                        } catch (IOException ignore) {}

                        // 엔트리 추가
                        try {
                            java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(rel);
                            zos.putNextEntry(ze);
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            // 로그만
                            System.out.println("zip skip error: " + rel + " - " + e.getMessage());
                        }
                    });
        }
    }

    // ====== (G) 유틸 ======
    private String tail(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : s.substring(s.length() - max);
    }

}
