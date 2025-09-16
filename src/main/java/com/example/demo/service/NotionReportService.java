package com.example.demo.service;

import com.example.demo.repository.NotionReportRepository;
import com.example.demo.vo.NotionReport;
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
public class NotionReportService {

    private final NotionReportRepository notionReportRepository;

    @Value("${notion.report-secret}")
    private String notionSecret;

    @Value("${notion.report-database-id}")
    private String reportDatabaseId;

    @Transactional
    public void saveAndCreateReport(NotionReport report) {
        // DB 저장
        notionReportRepository.saveReport(report);

        // Notion API 호출
        String notionPageId = createReport(report);

        // 생성된 pageId 업데이트
        notionReportRepository.updatePageId(report.getId(), notionPageId);
    }

    public String createReport(NotionReport report) {
        WebClient client = WebClient.builder()
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + notionSecret)
                .defaultHeader("Notion-Version", "2022-06-28")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> requestBody = Map.of(
                "parent", Map.of("database_id", reportDatabaseId),
                "properties", Map.of(
                        "id", Map.of("number", report.getId()),
                        "articleId", Map.of("number", report.getArticleId()),
                        "title", Map.of("title",
                                List.of(Map.of("text", Map.of("content", report.getTitle())))),
                        "nickName", Map.of("rich_text",
                                List.of(Map.of("text", Map.of("content", report.getNickName())))),
                        "email", Map.of("email", report.getEmail()),
                        "regDate", Map.of("date", Map.of("start", report.getRegDate())),
                        "body", Map.of("rich_text",
                                List.of(Map.of("text", Map.of("content", report.getBody()))))
                )
        );

        // 🚨 요청 JSON 로그 출력
        try {
            String jsonLog = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
            System.out.println("===== \uD83D\uDEA8 Notion API 요청 JSON (report) =====");
            System.out.println(jsonLog);
            System.out.println("==========================================");
        } catch (Exception e) {
            e.printStackTrace();
        }

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