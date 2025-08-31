package com.example.demo.controller;

import com.example.demo.service.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/notify")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(@RequestBody Map<String, String> request) {
        System.out.println("===== [POST] /api/DiFF/notify/send =====");
        System.out.println("📩 요청 바디: " + request);

        String token = request.get("token");
        String title = request.get("title");
        String body = request.get("body");

        System.out.println("🎯 추출된 값 → token: " + token);
        System.out.println("🎯 추출된 값 → title: " + title);
        System.out.println("🎯 추출된 값 → body: " + body);

        fcmService.sendMessage(token, title, body, null);

        System.out.println("✅ FCMService 호출 완료");

        return ResponseEntity.ok("알림 전송 완료");
    }
}
