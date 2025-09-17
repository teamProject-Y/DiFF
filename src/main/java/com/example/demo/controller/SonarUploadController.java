package com.example.demo.controller;

import com.example.demo.service.AnalysisOrchestrator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Controller;

import java.nio.file.Files;
import java.nio.file.Path;
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
        return ResponseEntity.accepted().body(Map.of("status", "queued", "jobId", jobId));
    }

    // 디버그 핑퐁 (이미 있으면 생략)
    @GetMapping("/upload/debug")
    public AnalysisOrchestrator.DebugInfo debug() {
        return orchestrator.getDebug();
    }
}