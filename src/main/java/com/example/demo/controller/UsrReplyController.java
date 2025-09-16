package com.example.demo.controller;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/reply")
public class UsrReplyController {

    @Autowired
    private ReplyService replyService;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private FcmService fcmService;

    @Autowired
    private MemberService memberService;

    @PostMapping("/doWrite")
    @ResponseBody
    public ResultData<Integer> doWrite(HttpServletRequest req,
                                       @RequestBody Reply reply) {

        System.out.println("===== 💬✏️ [Post] /api/DiFF/reply/doWrite =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        if (loginedMemberId == null) {
            return ResultData.from("F-400", "로그인 후 사용가능합니다.");
        }

        reply.setMemberId(loginedMemberId);

        if (reply.getArticleId() == null) {
            return ResultData.from("F-400", "articleId가 필요합니다.");
        } else if (reply.getBody() == null) {
            return ResultData.from("F-400", "내용을 입력하세요.");
        }

        // 댓글 저장
        int wr = replyService.doReplyWrtie(
                reply.getArticleId(),
                loginedMemberId,
                reply.getBody()
        );
        System.out.println(" 💬✏️ 댓글 저장 완료 wr=" + wr);

        // 글 작성자 조회
        Long articleWriter = articleService.getWriterIdByArticleId(reply.getArticleId());
        System.out.println(" 💬✏️ 글 작성자 ID : " + articleWriter);

        if (!articleWriter.equals(loginedMemberId)) {
            String title = "새 댓글 알림";
            String body = rq.getLoginedMemberNickName() + " wrote a new comment on your article.";

            // 글 작성자 Member 객체 가져오기
            Member articleWriterMember = memberService.getMemberById(articleWriter);

            if (articleWriterMember != null) {
                // DB에 알림 저장
                Notification notification = Notification.builder()
                        .memberId(articleWriter)
                        .type("REPLY")
                        .relId(reply.getArticleId())
                        .message(body)
                        .isRead(false)
                        .build();

                notificationService.saveNotification(notification);
                System.out.println(" 💬✏️✅ 알림 저장 완료 → 대상:" + articleWriter + ", 메시지:" + body);

                // FCM 발송
                if (articleWriterMember.isAllowReplyNotification()) {
                    if (articleWriterMember.getFcmToken() != null && !articleWriterMember.getFcmToken().isEmpty()) {
                        String targetToken = articleWriterMember.getFcmToken();

                        Map<String, String> data = new HashMap<>();
                        data.put("articleId", String.valueOf(reply.getArticleId()));
                        data.put("type", "REPLY");

                        fcmService.sendMessage(targetToken, title, body, data);
                        System.out.println(" 💬✏️📲 FCM 발송 완료 → 대상:" + articleWriter + ", token:" + targetToken);
                    } else {
                        System.out.println(" 💬✏️⚠️ 글 작성자의 FCM 토큰이 없음 → FCM 발송 불가");
                    }
                } else {
                    System.out.println(" 💬✏️⚠️ 댓글 알림 OFF → FCM 스킵");
                }
            }
        } else {
            System.out.println(" 💬✏️ 자기 자신의 글에 댓글");
        }

        return ResultData.from("S-1", "작성 성공", wr);
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> replyList(HttpServletRequest req,
                                                    @RequestParam Long articleId) {

        System.out.println("===== 💬🔢️ [Get] /api/DiFF/reply/list =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("💬🔢️👤 로그인 유저=" + loginedMemberId);

        List<Reply> replies = replyService.getReplies(articleId, loginedMemberId);

        Map<String, Object> result = new HashMap<>();
        result.put("replies", replies);
        result.put("totalCnt", replies.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResultData<Integer> modifyReply(HttpServletRequest req, @RequestBody Reply reply) {

        System.out.println("===== 💬✍️ [Post] /api/DiFF/reply/list =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        if (loginedMemberId == null) {
            return ResultData.from("F-1", "로그인 후 이용 가능합니다.");
        }

        Reply oldReply = replyService.getReplyById(reply.getId());

        if (oldReply == null) {
            return ResultData.from("F-2", "존재하지 않는 게시글입니다.");
        }

        if (!oldReply.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-3", "권한이 없습니다. 본인 글만 수정 가능합니다.");
        }

        int row = replyService.modifyReply(reply);

        if (row == 0) {
            return ResultData.from("F-4", "수정 실패", 0);
        }
        return ResultData.from("S-1", "수정 성공", row);
    }

    @DeleteMapping("/{id}")
    public ResultData<Integer> deleteReply(
            HttpServletRequest req, @PathVariable Long id) {

        System.out.println("===== 💬🗑️ [DELETE] /api/DiFF/reply/{id} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        Reply reply = replyService.getReplyById(id);
        if (reply == null) {
            return ResultData.from("F-404", "해당 댓글이 존재하지 않습니다.");
        }
        if (!reply.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-403", "해당 댓글에 대한 권한이 없습니다.");
        }

        int rows = replyService.deleteReply(id, loginedMemberId);
        if (rows == 0) {
            return ResultData.from("F-500", "댓글 삭제 실패");
        }

        return ResultData.from("S-1", "댓글 삭제 성공", rows);
    }

    // 좋아요
    @PostMapping("/like/{replyId}")
    public Map<String,Object> likeReply(HttpServletRequest req, @PathVariable Long replyId) {

        System.out.println("===== 💬👍 [Post] /api/DiFF/reply/like/{replyId} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.like("reply", replyId, memberId);

        System.out.println("💬👍 reply like success: " + row);

        return Map.of("relType","reply",
                "relId",replyId,
                "liked",true,
                "count", reactionService.count("reply", replyId));
    }

    // 취소
    @DeleteMapping("/like/{replyId}")
    public Map<String,Object> unlikeReply(HttpServletRequest req, @PathVariable Long replyId) {

        System.out.println("===== 💬👎 [Delete] /api/DiFF/reply/like/{replyId} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.unlike("reply", replyId, memberId);

        System.out.println("💬👎 reply unlike success: " + row);

        return Map.of("relType","reply",
                "relId",replyId,
                "liked",false,
                "count", reactionService.count("reply", replyId));
    }

    // 개수
    @GetMapping("/like/{replyId}")
    public Map<String,Object> getReplyLike(HttpServletRequest req, @PathVariable Long replyId) {

        System.out.println("===== 💬👍🔢 [Get] /api/DiFF/reply/like/{replyId} =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        return Map.of("relType","reply",
                "relId",replyId,
                "liked", reactionService.isLiked("reply", replyId, memberId),
                "count", reactionService.count("reply", replyId));
    }
}
