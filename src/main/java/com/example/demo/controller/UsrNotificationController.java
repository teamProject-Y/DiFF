package com.example.demo.controller;

import com.example.demo.service.NotificationService;
import com.example.demo.vo.Notification;
import com.example.demo.vo.Rq;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/DiFF/notification")
@RequiredArgsConstructor
public class UsrNotificationController {

    private final NotificationService notificationService;

    /**
     * 읽지 않은 알림이 있는지 여부 (빨간 점 표시 용)
     */
    @GetMapping("/unread")
    public boolean hasUnreadNotifications(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        return notificationService.hasUnread(memberId);
    }

    /**
     * 로그인한 회원의 알림 목록 조회
     */
    @GetMapping("/list")
    public List<Notification> getNotifications(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        return notificationService.getNotifications(memberId);
    }

    /**
     * 로그인한 회원의 모든 알림 읽음 처리
     */
    @PostMapping("/readAll")
    public void markAllAsRead(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        notificationService.markAllAsRead(memberId);
    }
}
