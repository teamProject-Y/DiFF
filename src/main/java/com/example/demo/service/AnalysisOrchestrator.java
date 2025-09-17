package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Service
public class AnalysisOrchestrator {

    private final SonarService sonarService;
    private final ThreadPoolTaskExecutor analysisExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AnalysisOrchestrator(SonarService sonarService,
                                ThreadPoolTaskExecutor analysisExecutor) {
        this.sonarService = sonarService;
        this.analysisExecutor = analysisExecutor;
    }

    /** meta JSON: memberId, repositoryId, draftId, diffId, lastChecksum, projectKey */
    public String enqueue(MultipartFile zip, String metaJson) {
        String jobId = UUID.randomUUID().toString();

        analysisExecutor.submit(() -> {
            try {
                Map<String, Object> meta = objectMapper.readValue(metaJson, Map.class);
                Long memberId     = toLong(meta.get("memberId"));
                Long repositoryId = toLong(meta.get("repositoryId"));
                Long draftId      = toLong(meta.get("draftId"));
                Long diffId       = toLong(meta.get("diffId"));
                String checksum   = (String) meta.get("lastChecksum");
                String projectKey = (String) meta.get("projectKey"); // 네가 동적으로 만드는 키

                String dir = sonarService.extractAndPrepare(zip, projectKey);
                sonarService.runSonarScanner(dir, projectKey);
                sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, checksum, projectKey);
                sonarService.deleteProject(projectKey); // 즉시 삭제 사용 시

            } catch (Exception e) {
                e.printStackTrace(); // TODO: 실패 상태 저장/로깅
            }
        });

        return jobId;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }
}
