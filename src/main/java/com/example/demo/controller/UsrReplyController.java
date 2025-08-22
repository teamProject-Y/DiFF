package com.example.demo.controller;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/reply")
public class UsrReplyController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ReplyService replyService;

    @Autowired
    private ReactionService reactionService;

    public UsrReplyController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @PostMapping("/doWrite")
    @ResponseBody
    public ResultData<Integer> doWrite(HttpServletRequest req,
                                       @RequestBody Reply reply) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        if(loginedMemberId == null) {
            return ResultData.from("F-400", "로그인 후 사용가능합니다..");
        }

        reply.setMemberId(loginedMemberId);

        System.out.println("\n===== \uD83D\uDC36\uD83D\uDC36 [POST] /reply/doWrite =====");

        if (reply.getArticleId() == null) {
            return ResultData.from("F-400", "articleId가 필요합니다.");
        } else if (reply.getBody() == null) {
            return ResultData.from("F-400", "내용을 입력하세요.");
        }

        // 작성
        int wr = replyService.doReplyWrtie(
                reply.getArticleId(),
                loginedMemberId,
                reply.getBody()
        );

        return ResultData.from("S-1", "작성 성공", wr);
    }

    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> replyList(HttpServletRequest req,
                                                    @RequestParam Long articleId) {

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("📌 댓글 리스트 호출됨, articleId=" + articleId);
        System.out.println("📌 로그인 유저=" + loginedMemberId);

        List<Reply> replies = replyService.getReplies(articleId, loginedMemberId);

        System.out.println("💬" + replies.size());

        Map<String, Object> result = new HashMap<>();
        result.put("replies", replies);
        result.put("totalCnt", replies.size());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResultData<Integer> modifyReply(HttpServletRequest req, @RequestBody Reply reply) {

        System.out.println("\n===== 🐶🐶 [POST] /api/DiFF/reply/modify =====");

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

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [DELETE] /api/DiFF/reply/" + id + " =====");

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

        System.out.println("post/like/reply 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.like("reply", replyId, memberId);

        System.out.println("like success: " + row);

        return Map.of("relType","reply",
                "relId",replyId,
                "liked",true,
                "count", reactionService.count("reply", replyId));
    }

    // 취소
    @DeleteMapping("/like/{replyId}")
    public Map<String,Object> unlikeReply(HttpServletRequest req, @PathVariable Long replyId) {

        System.out.println("delete/like/reply 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        int row = reactionService.unlike("reply", replyId, memberId);

        System.out.println("unlike success: " + row);

        return Map.of("relType","reply",
                "relId",replyId,
                "liked",false,
                "count", reactionService.count("reply", replyId));
    }

    // 개수
    @GetMapping("/like/{replyId}")
    public Map<String,Object> getReplyLike(HttpServletRequest req, @PathVariable Long replyId) {

        System.out.println("get/like/reply 진입");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        return Map.of("relType","reply",
                "relId",replyId,
                "liked", reactionService.isLiked("reply", replyId, memberId),
                "count", reactionService.count("reply", replyId));
    }
}
