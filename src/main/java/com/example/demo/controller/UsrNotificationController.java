package com.example.demo.controller;

import com.example.demo.service.MemberService;
import com.example.demo.service.NotificationService;
import com.example.demo.vo.Notification;
import com.example.demo.vo.ResultData;
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

    private final MemberService memberService;

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

    @PostMapping("/updateNotificationSetting")
    @ResponseBody
    public ResultData updateNotificationSetting(@RequestParam String type,
                                                @RequestParam boolean enabled,
                                                HttpServletRequest req) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        memberService.updateNotificationSetting(loginedMemberId, type, enabled);

        return ResultData.from("S-1", "알림 설정이 변경되었습니다.");
    }

}
