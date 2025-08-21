package com.example.demo.controller;
import com.example.demo.service.NotionInquiryService;
import com.example.demo.vo.NotionInquiry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/DiFF/notionInquiry")
public class UsrNotionInquiryController {

    @Autowired
    private NotionInquiryService notionInquiryService;

    @PostMapping("/saveInquiry")
    public ResponseEntity<String> saveInquiry(@RequestBody NotionInquiry inquiry) {
        System.out.println("🍗🍗 Received inquiry: " + inquiry);
        notionInquiryService.saveAndCreateInquiry(inquiry);
        return ResponseEntity.ok("문의사항이 정상적으로 접수되었습니다.");
    }

}
