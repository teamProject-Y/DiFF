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

    @GetMapping("/unread")
    public boolean hasUnreadNotifications(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        return notificationService.hasUnread(memberId);
    }

    @GetMapping("/list")
    public List<Notification> getNotifications(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        return notificationService.getNotifications(memberId);
    }

    @PostMapping("/readAll")
    public void markAllAsRead(HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();
        notificationService.markAllAsRead(memberId);
    }
}
