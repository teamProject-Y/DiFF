package com.example.demo.controller;

import com.example.demo.service.MemberService;
import com.example.demo.service.NotionReportService;
import com.example.demo.vo.Member;
import com.example.demo.vo.NotionReport;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/notionReport")
public class UsrNotionReportController {

    @Autowired
    private NotionReportService notionReportService;

    @Autowired
    private MemberService memberService;

    @PostMapping("/saveReport")
    public ResponseEntity<Map<String, Object>> saveReport(HttpServletRequest req,
                                                          @RequestBody NotionReport report) {

        System.out.println("===== 🚨✉️ [Post] /api/DiFF/notionReport/saveReport =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        // 로그인한 회원 정보
        Member member = memberService.getMemberById(memberId);

        // 신고자 정보 세팅
        report.setNickName(member.getNickName());
        report.setEmail(member.getEmail());

        // DB 저장 + Notion 페이지 생성
        notionReportService.saveAndCreateReport(report);

        return ResponseEntity.ok(Map.of("resultCode", "S-1",
                "message", "신고가 정상적으로 접수되었습니다.",
                "nickName", member.getNickName()
        ));
    }
}
