package com.example.demo.repository;

import com.example.demo.vo.Notification;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotificationRepository {

    void markAllAsRead(Long memberId);

    int hasUnread(Long memberId);

    List<Notification> getNotifications(Long memberId);

    void saveNotification(Notification notification);
}
