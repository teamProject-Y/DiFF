package com.example.demo.service;

import com.example.demo.repository.DraftRepository;
import com.example.demo.vo.Draft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GptService {

    private final WebClient openAiWebClient;
    private final DraftRepository draftRepository;

    public String makeDraft(String diff, Long repositoryId, Long memberId, String checksum, Long draftId) {

        if(!diff.isEmpty()) {
            System.out.println("🍔🍔2summarizeDiff 진입");
        }
        String prompt = "Summarize the following Git diff content, one line per code block:\n\n" + diff;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4.1-mini",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are an assistant that helps me write blog posts more easily by analyzing and summarizing code changes.\n" +
                                        "\n" +
                                        "Output language: Korean (한국어로 출력할 것).\n" +
                                        "\n" +
                                        "Summary rules:\n" +
                                        "1. The **summary** should focus on \"what has changed\" rather than structure, written as a **single noun phrase sentence**. Each item should be **numbered**.\n" +
                                        "2. Group summaries by **changed method/function/class**. If multiple code blocks are connected, merge them under one title.\n" +
                                        "3. For each change, provide the **core updated code block** right after the one-line summary. Do not shorten too much; show enough to infer the change. One-line summary **per code block**.\n" +
                                        "4. Ignore trivial changes such as debug logs, comment removals, or import reordering.\n" +
                                        "\n" +
                                        "Output example:\n" +
                                        "1. 명확하게 요약한 제목\n" +
                                        "[FileName.java - methodName]\n" +
                                        "```java\n" +
                                        "updated code here\n" +
                                        "```\n" +
                                        "\n" +
                                        "2. 명확하게 요약한 제목\n" +
                                        "[Repository.xml - queryName]\n" +
                                        "```xml\n" +
                                        "updated code here\n" +
                                        "```\n"
                        ),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.2
        );
        try {
            Map<String, Object> response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            String content = (String) message.get("content");

            System.out.println("🐒🐒 content: " + content);

            // DB 저장
            Draft draft = Draft.builder()
                    .id(draftId) // 이미 생성된 draftId 사용
                    .memberId(memberId)
                    .repositoryId(repositoryId)
                    .checksum(checksum)
                    .body(content)
                    .build();

            draftRepository.updateDraft(draft);
            System.out.println("✅ 초안 업데이트 완료 - draftId=" + draftId);

            return content;

        } catch (WebClientResponseException e) {
            System.out.println("[GPT 응답 오류]: " + e.getResponseBodyAsString());
            return "[GPT 응답 오류]: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            System.out.println("[GPT 예외 발생]: " + e.getMessage());
            return "[GPT 예외 발생]: " + e.getMessage();
        }
    }
}