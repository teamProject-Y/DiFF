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

    public String makeDraft(String diff, Long repositoryId, Long memberId, String checksum) {
        System.out.println("🍔🍔 DiFF 있음 ?  : "+diff);
        System.out.println("🍔🍔2summarizeDiff 진입");
        String prompt = "다음 Git diff 내용을 한 줄로 요약해줘:\n\n" + diff;

        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "너는 내가 블로그 작성을 편하게 하기 위해, 코드 변경 사항을 분석하고 간결하게 요약해주는 도우미야.\n" +
                                        /*"너는 지금부터 내가 보내는 diff를 계속 기억해야 해. 각 요청은 이전 요청과 연결된 연속적인 변경사항일 수 있어.\n" +*/
                                        "요약 형식:\n" +
                                        "1. **변경 요약**은 구조보다는 \"무엇이 달라졌는지\"에 초점을 맞춰, **명사형 문장**으로 정리하고, 각 항목은 **번호를 매긴다**.\n" +
                                        "2. **변경된 메서드/함수/클래스 단위로 묶어서** 설명한다. 다른 코드 블럭 중 연결되는 내용은 하나의 제목으로 묶는다..\n" +
                                        "3. 각 변경사항마다 **수정 후 핵심 코드 블럭**을 함께 출력하되, 너무 짧게 생략하지 말고 **변화를 유추할 수 있을 만큼만** 보여준다.\n" +
                                        "4. **디버깅용 로그, 주석 제거, import 순서 변경 등 사소한 변화는 생략한다.**\n" +
                                        "\n" +
                                        "출력 예시:\n" +
                                        "1. 명확하게 요약한 제목\n" +
                                        "[파일명 - 메서드명]\n" +
                                        "```변경 후 코드```\n" +
                                        "```\n" +
                                        "\n" +
                                        "2. 명확하게 요약한 제목"+
                                        "[파일명 - 메서드명]\n" +
                                        "```변경 후 코드```\n" ),
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

            // ✅ DB 저장
            Draft draft = Draft.builder()
                    .memberId(memberId)
                    .repositoryId(repositoryId)
                    .checksum(checksum)
                    .title(null) // 필요 시 추출 또는 별도 입력
                    .body(content)
                    .regDate(LocalDateTime.now())
                    .build();

            draftRepository.insertDraft(draft);
            System.out.println("✅ 초안 저장 완료 - ID: " + draft.getId());

            return content;

        } catch (WebClientResponseException e) {
            return "[GPT 응답 오류]: " + e.getResponseBodyAsString();
        } catch (Exception e) {
            return "[GPT 예외 발생]: " + e.getMessage();
        }
    }
}