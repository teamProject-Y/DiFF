package com.example.demo.service;

import com.example.demo.repository.NotionInquiryRepository;
import com.example.demo.vo.NotionInquiry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotionInquiryService {

    private final NotionInquiryRepository notionInquiryRepository;

    @Value("${notion.secret}")
    private String notionSecret;

    @Value("${notion.database-id}")
    private String databaseId;

    @Transactional
    public void saveAndCreateInquiry(NotionInquiry inquiry) {

        notionInquiryRepository.saveInquiry(inquiry);

        String notionPageId = createInquiry(inquiry);

        notionInquiryRepository.updatePageId(inquiry.getId(), notionPageId);
    }

    public String createInquiry(NotionInquiry inquiry) {
        WebClient client = WebClient.builder()
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + notionSecret)
                .defaultHeader("Notion-Version", "2022-06-28")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = Map.of(
                "parent", Map.of("database_id", databaseId),
                "properties", Map.of(
                        "id", Map.of("number", inquiry.getId()), // ✅ 숫자 타입
                        "title", Map.of("title",
                                List.of(Map.of("text", Map.of("content", inquiry.getTitle())))),
                        "nickName", Map.of("rich_text",
                                List.of(Map.of("text", Map.of("content", inquiry.getNickName())))),
                        "email", Map.of("email", inquiry.getEmail()),
                        "regDate", Map.of("date", Map.of("start", inquiry.getRegDate())),
                        "body", Map.of("rich_text",
                                List.of(Map.of("text", Map.of("content", inquiry.getBody()))))
                )
        );

        String response = client.post()
                .uri("/pages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            JsonNode json = new ObjectMapper().readTree(response);
            return json.get("id").asText(); // Notion pageId 추출
        } catch (Exception e) {
            throw new RuntimeException("Notion API 응답 파싱 실패: " + response, e);
        }
    }
}
