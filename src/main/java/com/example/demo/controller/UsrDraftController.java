package com.example.demo.controller;

import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/draft")
public class UsrDraftController {

    @Autowired
    private Rq rq;
    @Autowired
    private DraftService draftService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private GptService gptService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DiffService diffService;
    @Autowired
    private SonarService sonarService;

    @PostMapping("/verifyGitUser")
    @ResponseBody
    public ResultData verifyGitUser(@RequestBody Map<String, String> requestMap) {

        System.out.println("👹 verifygituser 진입");

        String email = requestMap.get("email");
        Integer verifiedMemberId = memberService.isVerifiedUser(email);

        System.out.println(verifiedMemberId);

        if(verifiedMemberId != null) {
            System.out.println("git email로 찾은 memberID: " + verifiedMemberId);
            return ResultData.from("S-1", "사용자 인증 완료", "인증된 사용자 id", verifiedMemberId);
        }else {
            System.err.println("git email로 찾은 member 없음");
            return ResultData.from("F-1", "사용자 인증 실패");
        }
    }

    @PostMapping("/mkRepo")
    @ResponseBody
    public ResultData mkRepo(@RequestBody Map<String, Object> param) {

        Long memberId = (Long) param.get("memberId");
        String repoName = (String) param.get("repoName");
        String firstCommit = (String) param.get("firstCommit");
        System.out.println(memberId);
        System.out.println(repoName);
        System.out.println(firstCommit);

        boolean existsRepoName = repositoryService.existsByMemberIdAndRepoName(memberId, repoName);
        if(!existsRepoName) return ResultData.from("F-1", "이미 존재하는 리포지토리 이름");

        repositoryService.makeRepository(memberId, repoName, firstCommit);
        int repoId = repositoryService.getLastInsertId();

        return ResultData.from("S-1", "리포지토리 생성", "repositoryID", repoId);
    }

    @PostMapping("/isUsableRepoName")
    @ResponseBody
    public ResultData isUsableRepoName(@RequestBody Map<String, Object> param) {

        int memberId = (Integer) param.get("memberId");
        String repoName = (String) param.get("repoName");

        boolean isUsableRepoName = draftService.existsByMemberIdAndRepoName(memberId, repoName);
        if(isUsableRepoName){
            return ResultData.from("S-1", "리포지토리 이름 중복 여부", "가능", isUsableRepoName);
        }else {
            return ResultData.from("F-1", "리포지토리 이름 중복 여부", "불가능", isUsableRepoName);
        }
    }


    @PostMapping("/mkDraft")
    @ResponseBody
    public ResultData<Map<String, Object>> mkDraft(@RequestBody Map<String, Object> param) {
        System.out.println("🍔 mkDraft 메서드 진입");
        System.out.println("🍔 param: " + param);

        try {
            Long memberId = ((Number) param.get("memberId")).longValue();
            Long repositoryId = ((Number) param.get("repositoryId")).longValue();
            String checksum = (String) param.get("checksum"); // ✅ 체크섬 받기

            // 1. Draft 생성
            Draft draft = Draft.builder()
                    .memberId(memberId)
                    .repositoryId(repositoryId)
                    .title("(자동 생성)")
                    .body("")
                    .checksum(checksum) // ✅ draft에도 넣음
                    .build();

            Long draftId = draftService.saveDraft(draft);

            // 2. Diff 생성
            Diff diff = Diff.builder()
                    .draftId(draftId)
                    .checksum(checksum) // ✅ diff에도 넣음
                    .build();
            Long diffId = diffService.saveDiff(diff);

            System.out.println("✅ mkDraft Draft 생성 완료 → draftId=" + draftId + ", diffId=" + diffId);

            Map<String, Object> resultData = new HashMap<>();
            resultData.put("draftId", draftId);
            resultData.put("diffId", diffId);
            resultData.put("checksum", checksum);

            return ResultData.from("S-1", "Draft + Diff + checksum 저장 완료", resultData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.from("F-1", "Draft 생성 실패", null);
        }
    }

    @PostMapping("/receiveDiff")
    @ResponseBody
    public ResultData<String> receiveDiff(@RequestBody Map<String, Object> param) {
        System.out.println("📥 receiveDiff 진입");
        System.out.println("📥 param: " + param);

        try {
            Long memberId = ((Number) param.get("memberId")).longValue();
            Long repositoryId = ((Number) param.get("repositoryId")).longValue();
            Long draftId = ((Number) param.get("draftId")).longValue();
            Long diffId = ((Number) param.get("diffId")).longValue();
            String lastChecksum = (String) param.get("lastChecksum");
            String diffText = (String) param.get("diff");

            System.out.println("➡️ memberId     = " + memberId);
            System.out.println("➡️ repositoryId = " + repositoryId);
            System.out.println("➡️ draftId      = " + draftId);
            System.out.println("➡️ diffId       = " + diffId);
            System.out.println("➡️ lastChecksum = " + lastChecksum);

            if (diffText == null || diffText.trim().isEmpty()) {
                System.out.println("⚠️ diffText 비어있음 → 실패 반환");
                return ResultData.from("F-1", "diff 내용이 비어있습니다.");
            }

            // 1. GPT 호출 → Draft 본문 생성
            System.out.println("🧠 GPT 호출 시작...");
            String draftBody = gptService.makeDraft(diffText, repositoryId, memberId, lastChecksum, draftId);
            System.out.println("🧠 GPT 호출 완료. 생성된 draftBody 길이 = " + (draftBody != null ? draftBody.length() : 0));

            // 2. Draft 업데이트
            Draft draft = Draft.builder()
                    .id(draftId)
                    .memberId(memberId)
                    .repositoryId(repositoryId)
                    .body(draftBody)
                    .checksum(lastChecksum)
                    .build();
            draftService.updateDraft(draft);

            // 3. Diff 업데이트
            Diff diffEntity = Diff.builder()
                    .id(diffId)
                    .draftId(draftId)
                    .checksum(lastChecksum)
                    .build();
            diffService.updateDiff(diffEntity);

            // 4. 알림 처리
            Member member = memberService.getMemberById(memberId);
            System.out.println("👤 대상 사용자 조회: " + (member != null ? member.getNickName() : "없음"));

            if (member != null) {
                String message = "Your draft has been created.";

                //  DB 알림 저장
                Notification notification = Notification.builder()
                        .memberId(member.getId())
                        .type("DRAFT")
                        .message(message)
                        .isRead(false)
                        .relId(draftId)
                        .build();

                notificationService.saveNotification(notification);

                //  FCM 발송
                if (member.isAllowDraftNotification()) {
                    if (member.getFcmToken() != null && !member.getFcmToken().isEmpty()) {
                        System.out.println("📲 FCM 발송 시작 → token=" + member.getFcmToken());
                        fcmService.sendMessage(
                                member.getFcmToken(),
                                "Your draft has been created.",
                                message,
                                null
                        );
                        System.out.println("✅ FCM 알림 전송 완료");
                    } else {
                        System.out.println("⚠️ fcmToken 없음 → FCM 발송 스킵");
                    }
                } else {
                    System.out.println("⚠️ Draft 알림 OFF → 푸시 스킵 (DB 저장은 완료)");
                }
            }

            return ResultData.from("S-1", "Draft 업데이트 및 분석 성공", draftBody);

        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.from("F-2", "Diff 처리 실패", null);
        }
    }


    @GetMapping("/drafts")
    public ResponseEntity<Map<String, Object>> getDrafts() {
        System.out.println("📥 /api/DiFF/draft/drafts 요청 도착");

        Number memberIdNum = (Number) rq.getLoginedMemberId();
        Long memberId = memberIdNum.longValue();

        List<Draft> drafts = draftService.getDraftsByMember(memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("drafts", drafts);
        System.out.println("" + result);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResultData<Integer> deleteDraft(
            HttpServletRequest req, @PathVariable Long id) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== \uD83D\uDC36 \uD83D\uDC36 [DELETE] /api/DiFF/draft/" + id + " =====");

        Draft draft = draftService.getDraftById(id);
        if (draft == null) {
            return ResultData.from("F-404", "해당 게시글이 존재하지 않습니다.");
        }
        if (!draft.getMemberId().equals(memberId)) {
            return ResultData.from("F-403", "해당 게시글에 대한 권한이 없습니다.");
        }

        int rows = draftService.deleteDraft(id, memberId);
        if (rows == 0) {
            return ResultData.from("F-500", "게시글 삭제 실패");
        }

        return ResultData.from("S-1", "게시글 삭제 성공", rows);
    }

    @GetMapping("/{id}")
    public ResultData<Draft> getDraftById(HttpServletRequest req, @PathVariable Long id) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        Draft draft = draftService.getDraftById(id);

        if (draft == null) {
            return ResultData.from("F-404", "해당 임시저장이 존재하지 않습니다.");
        }
        if (!draft.getMemberId().equals(memberId)) {
            return ResultData.from("F-403", "해당 임시저장에 접근 권한이 없습니다.");
        }
        return ResultData.from("S-1", "임시저장 조회 성공", draft);
    }

    @PostMapping("/save")
    public ResultData<Long> saveDraft(HttpServletRequest req, @RequestBody Draft draft) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("📥 [Controller] /draft/save 요청 도착");
        System.out.println("📥 [Controller] 요청 데이터: id=" + draft.getId()
                + ", title=" + draft.getTitle()
                + ", body=" + (draft.getBody() != null
                ? draft.getBody().substring(0, Math.min(20, draft.getBody().length())) + "..."
                : "null")
                + ", repositoryId=" + draft.getRepositoryId()
                + ", isPublic=" + draft.getIsPublic());

        draft.setMemberId(memberId);

        if (draft.getIsPublic() == null) {
            draft.setIsPublic(true);
        }
        Long draftId = draftService.saveDraft(draft);

        System.out.println("📤 [Controller] save Draft 완료 → draftId=" + draftId);

        return ResultData.from("S-1", "임시저장이 완료되었습니다.", draftId);
    }

}
