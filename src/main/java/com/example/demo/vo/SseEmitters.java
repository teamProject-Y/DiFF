package com.example.demo.vo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 유저별로 관리
@Component
@Slf4j
public class SseEmitters {
    // userId -> emitter 리스트
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 연결 수립 시 호출
    public SseEmitter add(Long userId, SseEmitter emitter) {
        emitters
                .computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> emitters.getOrDefault(userId, List.of()).remove(emitter));
        emitter.onTimeout(()  ->  emitter.complete());

        return emitter;
    }

    // 특정 유저에게만 알림 보내기
    public void notifyUser(Long userId, String eventName, Object data) {
        List<SseEmitter> list = emitters.getOrDefault(userId, List.of());
        for (SseEmitter em : list) {
            try {
                em.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                // 필요 시 로그 찍고 제거
                emitters.get(userId).remove(em);
            }
        }
    }


}