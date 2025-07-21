package com.example.demo.controller;

import com.example.demo.vo.Member;
import com.example.demo.vo.SseEmitters;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Controller
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitters sseEmitters;

    // "/sse/connect"에 EventSource로 접속
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@AuthenticationPrincipal Member member) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃 무제한
        // 로그인한 userId로 등록

        sseEmitters.add((long) member.getId(), emitter);

        // 초기 연결 확인 이벤트
        try {
            // 초깃값으로 "연결됨!" 이벤트 전송
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("연결됨!"));
        } catch (IOException ignored) {}
        return emitter;
    }

}