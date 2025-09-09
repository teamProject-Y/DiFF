package com.example.demo.controller;
import com.example.demo.service.MemberService;
import com.example.demo.service.NotionInquiryService;
import com.example.demo.vo.Member;
import com.example.demo.vo.NotionInquiry;
import com.example.demo.vo.Rq;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/notionInquiry")
public class UsrNotionInquiryController {

    @Autowired
    private NotionInquiryService notionInquiryService;

    @Autowired
    private MemberService memberService;

    @PostMapping("/saveInquiry")
    public ResponseEntity<Map<String, Object>> saveInquiry(HttpServletRequest req, @RequestBody NotionInquiry inquiry) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result", "fail", "message", "Please log in and try again."));
        }

        Member member = memberService.getMemberById(memberId);
        inquiry.setNickName(member.getNickName());

        notionInquiryService.saveAndCreateInquiry(inquiry);

        return ResponseEntity.ok(Map.of(
                "result", "success",
                "message", "Your inquiry has been submitted.",
                "nickName", member.getNickName()
        ));
    }
}
