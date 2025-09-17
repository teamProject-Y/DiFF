package com.example.demo.controller;

import com.example.demo.service.AnalysisOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class SonarUploadController {

    @Value("${sonar.host}")
    private String sonarHost;

    private final AnalysisOrchestrator orchestrator;
    private static final Logger log = LoggerFactory.getLogger(SonarUploadController.class);

    public SonarUploadController(AnalysisOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile zipFile,
            @RequestPart("meta") String metaJson) {

        System.out.println("===== 📂 [Post] /upload =====");

        String jobId = orchestrator.enqueue(zipFile, metaJson);
        return ResponseEntity.accepted().body(Map.of(
                "status", "queued",
                "jobId", jobId
        ));

//        try {
//            // JSON → Map 변환
//            ObjectMapper mapper = new ObjectMapper();
//            Map<String, Object> param = mapper.readValue(metaJson, Map.class);
//
//            System.out.println("📂 uploadSource param = " + param);
//
//            Long memberId = ((Number) param.get("memberId")).longValue();
//            Long repositoryId = ((Number) param.get("repositoryId")).longValue();
//            Long draftId = ((Number) param.get("draftId")).longValue();
//            Long diffId = ((Number) param.get("diffId")).longValue();
//            String lastChecksum = (String) param.get("lastChecksum");
//
//            String projectKey = "M-" + memberId + "_R-" + repositoryId + "_A-" + draftId + "_C-" + lastChecksum;
//            System.err.println("📂 projectKey: " + projectKey);
//
//            // 압축 해제
//            String extractedPath = sonarService.extractAndPrepare(zipFile, projectKey);
//            System.out.println("📂 압축 해제 위치: " + extractedPath);
//
//            // 분석 실행
//            sonarService.runSonarScanner(extractedPath, projectKey);
//            sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, lastChecksum, projectKey);
//
//            // 결과 조회
//            String result = sonarService.getAnalysisResult(projectKey);
//            System.out.println("📂 분석 결과: " + result);
//
//            grantProjectAdminPermission(projectKey);
//            Thread.sleep(2000);
//            sonarService.deleteProject(projectKey);
//
//            return ResponseEntity.ok(result);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.internalServerError().body("분석 중 오류 발생: " + e.getMessage());
//        }
    }

    private void grantProjectAdminPermission(String projectKey) {
//        String sonarBaseUrl = http://localhost:9000;
        String apiEndpoint = sonarHost + "/api/permissions/add_user";

        String login = "admin";
        String password = "teamprojectY1!";

        try {
            String urlWithParams = apiEndpoint
                    + "?login=" + URLEncoder.encode(login, StandardCharsets.UTF_8)
                    + "&permission=admin"
                    + "&projectKey=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);

            HttpURLConnection connection = (HttpURLConnection) new URL(urlWithParams).openConnection();
            connection.setRequestMethod("POST");
            String basicAuth = "Basic " + Base64.getEncoder()
                    .encodeToString((login + ":" + password).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", basicAuth);

            int responseCode = connection.getResponseCode();
            if (responseCode == 204) {
                System.out.println("📊 프로젝트 관리자 권한 부여 완료: " + projectKey);
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();
                System.out.println("📊 권한 부여 실패: " + response);
            }

        } catch (IOException e) {
            System.out.println("📊 권한 부여 중 예외 발생: " + e.getMessage());
        }
    }

    @GetMapping("/upload/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        try {
            var info = Optional.ofNullable(orchestrator.getDebug())
                    .orElseGet(AnalysisOrchestrator.DebugInfo::idle);

            // 로그도 NPE 없게 String.valueOf 사용
            log.info("[DEBUG] jobId={}, status={}, step={}, queuedAt={}, startedAt={}, finishedAt={}, error={}",
                    String.valueOf(info.jobId), String.valueOf(info.status), String.valueOf(info.step),
                    String.valueOf(info.queuedAt), String.valueOf(info.startedAt),
                    String.valueOf(info.finishedAt), String.valueOf(info.error));

            Map<String,Object> body = new LinkedHashMap<>();
            body.put("jobId", info.jobId);
            body.put("status", info.status);
            body.put("step", info.step);
            body.put("queuedAt", info.queuedAt);
            body.put("startedAt", info.startedAt);
            body.put("finishedAt", info.finishedAt);
            body.put("error", info.error);
            // 참고로 가장 최근 jobId도 같이
            body.put("lastJobId", orchestrator.getLastJobId());

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("debug endpoint error", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "debug_failed",
                    "detail", e.getMessage()
            ));
        }
    }
}