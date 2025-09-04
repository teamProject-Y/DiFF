package com.example.demo.controller;

import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResultData<String> receiveDiff(@RequestBody Map<String, Object> param) {
        System.out.println("receiveDiff 메서드 진입");
        System.out.println("🍔param: " + param);

        Number memberIdNum = (Number) param.get("memberId");
        Number repositoryIdNum = (Number) param.get("repositoryId");

        Long memberId = memberIdNum.longValue();
        Long repositoryId = repositoryIdNum.longValue();
        String lastChecksum = (String) param.get("lastChecksum");
        String diff = (String) param.get("diff");

        System.out.println("memberId: " + memberId);
        System.out.println("lastChecksum: " + lastChecksum);

        if (diff == null || diff.trim().isEmpty()) {
            System.err.println("diff 없음!!!!!!!!!!!!!!!!!!!!");
            return ResultData.from("F-1", "diff 내용이 비어있습니다.");
        }

        String draft;
        try {
            draft = gptService.makeDraft(diff, repositoryId, memberId, lastChecksum);

            Member member = memberService.getFcmTokenById(memberId);
            String message = "Your changes have been saved as a draft.";

            if (member != null) {
                // 1. FCM 발송
                if (member.getFcmToken() != null && !member.getFcmToken().isEmpty()) {
                    fcmService.sendMessage(
                            member.getFcmToken(),
                            "Draft created successfully",
                            message,
                            null
                    );
                    System.out.println("✅ FCM 알림 전송 완료");
                } else {
                    System.out.println("⚠️ fcmToken 없음 → 알림 생략");
                }

                // 2. DB에 알림 저장 (빨간점 표시용)
                Notification notification = Notification.builder()
                        .memberId(member.getId())
                        .type("DRAFT")
                        .message(message)
                        .isRead(false)  // 읽지 않았으므로 빨간 점 표시
                        .build();

                notificationService.saveNotification(notification);
                System.out.println("✅ Draft 알림 DB 저장 완료 → 빨간점 표시 가능");
            }

        } catch (Exception e) {
            return ResultData.from("F-2", "초안 생성에 실패했습니다.", "error", e.getMessage());
        }
        return ResultData.from("S-1", "커밋 diff 수신 및 초안 생성에 성공했습니다.", "draft", draft);
    }

    @GetMapping("/drafts")
    public ResponseEntity<Map<String, Object>> getDrafts() {
        System.out.println("📥 /api/DiFF/draft/drafts 요청 도착");

        Number memberIdNum = (Number) rq.getLoginedMemberId();
        Long memberId = memberIdNum.longValue();

        List<Draft> drafts = draftService.getDraftsByMember(memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("drafts", drafts);
        System.out.println(""+result);
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
                + ", body=" + (draft.getBody() != null ? draft.getBody().substring(0, Math.min(20, draft.getBody().length())) + "..." : "null")
                + ", repositoryId=" + draft.getRepositoryId());

        draft.setMemberId(memberId);

        Long draftId = draftService.saveDraft(draft);

        System.out.println("📤 [Controller] saveDraft 완료 → draftId=" + draftId);

        return ResultData.from("S-1", "임시저장이 완료되었습니다.", draftId);
    }


}
