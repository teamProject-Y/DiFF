package com.example.demo.service;

import com.example.demo.repository.NotificationRepository;
import com.example.demo.vo.Member;
import com.example.demo.vo.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    /**
     * 읽지 않은 알림이 있는지 확인 (빨간 점 표시 용)
     */
    public boolean hasUnread(Long memberId) {
        return notificationRepository.hasUnread(memberId) > 0;
    }

    /**
     * 알림 전체 조회
     */
    public List<Notification> getNotifications(Long memberId) {
        return notificationRepository.getNotifications(memberId);
    }

    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
        System.out.println("✅ memberId=" + memberId + " 알림 전체 읽음 처리 완료");
    }
    public void saveNotification(Notification notification) {
        notificationRepository.saveNotification(notification);
        System.out.println("✅ 알림 저장 완료 → " + notification.getMessage());
    }

    public void saveNotification(Long articleOwnerId, Long loginedMemberId, String reply, Long articleId, String body) {
    }
}
