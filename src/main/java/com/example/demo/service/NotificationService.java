package com.example.demo.service;

import com.example.demo.vo.SseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SseEmitters sseEmitters;

    // 배치 작업이나 파일 처리 끝나는 곳에서 호출
    public void notifyWorkDone(Long userId, String message) {
        Map<String, Object> data = Map.of("userId", userId, "msg", message);
//        sseEmitters.noti("작업 완료", data);
    }

}