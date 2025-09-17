package com.example.demo.controller;

import com.example.demo.service.AnalysisOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            @RequestPart("file") MultipartFile zip,
            @RequestPart("meta") String metaJson
    ) throws Exception {
        // 요청 스레드에서 먼저 영속 임시파일로 보관
        Path saved = Files.createTempFile("upload-", ".zip");
        zip.transferTo(saved.toFile());

        String jobId = orchestrator.enqueueFile(saved.toString(), metaJson);

        DEBUG.put("jobId", jobId);
        DEBUG.put("status", "QUEUED");
        DEBUG.put("step", "UPLOAD_ACCEPTED");
        DEBUG.put("updatedAt", java.time.Instant.now().toString());

        return ResponseEntity.accepted().body(Map.of("status", "queued", "jobId", jobId));
    }

    // SonarUploadController 클래스 안 (필드 영역 어딘가)
    private static final java.util.Map<String, Object> DEBUG = new java.util.LinkedHashMap<>();
    static {
        DEBUG.put("jobId", null);
        DEBUG.put("status", "IDLE");
        DEBUG.put("step", "IDLE");
        DEBUG.put("error", null);
        DEBUG.put("buildCmd", null);
        DEBUG.put("workdir", null);
        DEBUG.put("updatedAt", java.time.Instant.now().toString());
    }

    // JSON으로 그대로 뱉기 (뷰 포워딩 금지)
    @org.springframework.web.bind.annotation.GetMapping(
            value = "/upload/debug",
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> debug() {
        return DEBUG;
    }

}