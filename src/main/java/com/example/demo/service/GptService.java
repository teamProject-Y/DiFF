package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GptService {

    private final WebClient openAiWebClient;

    public String summarizeDiff(String diff) {
        String prompt = "다음 Git diff 내용을 한 줄로 요약해줘:\n\n" + diff;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content", "너는 소스 코드 변경을 분석해서 요약해주는 AI야."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );

        try {
            Map<String, Object> response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> {
                        // 👉 응답 상태와 본문 출력
                        return clientResponse.bodyToMono(String.class)
                                .doOnNext(errorBody -> System.out.println("❌ GPT 에러 응답 본문:\n" + errorBody))
                                .flatMap(errorBody -> {
                                    System.out.println("❌ 상태 코드: " + clientResponse.statusCode());
                                    return clientResponse.createException();
                                });
                    })
                    .bodyToMono(Map.class)
                    .doOnNext(body -> System.out.println("✅ GPT 응답 전체:\n" + body))
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");

        } catch (Exception e) {
            System.out.println("❗ 예외 발생: " + e.getMessage());
            return "[GPT 요약 실패]: " + e.getMessage();
        }
    }

}
