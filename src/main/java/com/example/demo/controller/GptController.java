package com.example.demo.controller;

import com.example.demo.service.GptService;
import com.example.demo.vo.ResultData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gpt")
public class GptController {

    private final GptService gptService;

    @PostMapping("/summarizeDiff")
    public ResultData summarizeDiff(@RequestBody Map<String, Object> param) {
        String diff = (String) param.get("diff");

        if (diff == null || diff.trim().isEmpty()) {
            return ResultData.from("F-1", "diff 내용이 비어있습니다.");
        }

        try {
            String summary = gptService.summarizeDiff(diff);
            return ResultData.from("S-1", "GPT 요약 성공", "summary", summary);
        } catch (Exception e) {
            return ResultData.from("F-2", "GPT 요약 실패", "error", e.getMessage());
        }
    }

    @PostMapping("/summarize")
    public ResponseEntity<String> summarize(@RequestBody String diff) {
        String summary = gptService.summarizeDiff(diff);
        return ResponseEntity.ok(summary);
    }
}
