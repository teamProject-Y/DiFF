package com.example.demo.controller;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.*;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/reply")
public class ReplyController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ReplyService replyService;

    public ReplyController(BeforeActionInterceptor beforeActionInterceptor) {
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

}
