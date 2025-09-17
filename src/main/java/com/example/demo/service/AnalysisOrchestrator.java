package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AnalysisOrchestrator {

    private final SonarService sonarService;
    private final ThreadPoolTaskExecutor analysisExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 디버깅용
    private final AtomicReference<String> lastJobId = new AtomicReference<>("");
    private final AtomicReference<DebugInfo> debug = new AtomicReference<>(DebugInfo.idle());

    // 디버그 모델
    public static class DebugInfo {
        public String jobId;
        public String status;    // IDLE/QUEUED/RUNNING/SUCCESS/FAILED
        public String step;      // EXTRACT/SONAR/RESULT_DB/CLEANUP/DONE/ERROR
        public Instant queuedAt;
        public Instant startedAt;
        public Instant finishedAt;
        public String error;
        public static DebugInfo idle() {
            DebugInfo d = new DebugInfo();
            d.jobId = "";
            d.status = "IDLE";
            d.step = "";
            return d;
        }
    }

    public DebugInfo getDebug() {
        DebugInfo d = debug.get();
        return (d != null) ? d : DebugInfo.idle();
    }

    public String getLastJobId() { return lastJobId.get(); }

    public AnalysisOrchestrator(SonarService sonarService,
                                ThreadPoolTaskExecutor analysisExecutor) {
        this.sonarService = sonarService;
        this.analysisExecutor = analysisExecutor;
    }

    /** meta JSON: memberId, repositoryId, draftId, diffId, lastChecksum, projectKey */
//    public String enqueue(MultipartFile zip, String metaJson) {
//        String jobId = UUID.randomUUID().toString();
//
//        // 디버깅용
//        lastJobId.set(jobId);
//        var dq = new DebugInfo();
//        dq.jobId = jobId;
//        dq.status = "QUEUED";
//        dq.step = "QUEUED";
//        dq.queuedAt = Instant.now();
//        debug.set(dq);
//
//        analysisExecutor.submit(() -> {
//            try {
//                // RUNNING 시작
//                var dr = new DebugInfo();
//                dr.jobId = jobId;
//                dr.status = "RUNNING";
//                dr.step = "EXTRACT";
//                dr.queuedAt = dq.queuedAt;
//                dr.startedAt = Instant.now();
//                debug.set(dr);
//
//                Map<String,Object> meta = objectMapper.readValue(metaJson, Map.class);
//                String projectKey = (String) meta.get("projectKey");
//                Long memberId     = toLong(meta.get("memberId"));
//                Long repositoryId = toLong(meta.get("repositoryId"));
//                Long draftId      = toLong(meta.get("draftId"));
//                Long diffId       = toLong(meta.get("diffId"));
//                String checksum   = (String) meta.get("lastChecksum");
//
//                // EXTRACT
//                String dir = sonarService.extractAndPrepare(zip, projectKey);
//
//                // SONAR
//                dr.step = "SONAR";
//                debug.set(dr);
//                sonarService.runSonarScanner(dir, projectKey);
//
//                // RESULT_DB
//                dr.step = "RESULT_DB";
//                debug.set(dr);
//                sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, checksum, projectKey);
//
//                // CLEANUP
//                dr.step = "CLEANUP";
//                debug.set(dr);
//                sonarService.deleteProject(projectKey);
//
//                // SUCCESS
//                dr.status = "SUCCESS";
//                dr.step = "DONE";
//                dr.finishedAt = Instant.now();
//                debug.set(dr);
//            } catch (Exception e) {
//                var df = new DebugInfo();
//                df.jobId = jobId;
//                df.status = "FAILED";
//                df.step = "ERROR";
//                df.queuedAt = dq.queuedAt;
//                df.startedAt = dq.startedAt;
//                df.finishedAt = Instant.now();
//                df.error = e.getMessage();
//                debug.set(df);
//            }
//        });
//
//        return jobId;
//    }

    public String enqueueFile(String zipPath, String metaJson) {
        String jobId = java.util.UUID.randomUUID().toString();

        // 디버그: QUEUED
        lastJobId.set(jobId);
        DebugInfo dq = DebugInfo.idle();
        dq.jobId = jobId;
        dq.status = "QUEUED";
        dq.step = "QUEUED";
        dq.queuedAt = java.time.Instant.now();
        debug.set(dq);

        analysisExecutor.submit(() -> {
            DebugInfo d = new DebugInfo();
            d.jobId = jobId;
            d.status = "RUNNING";
            d.step = "EXTRACT";
            d.queuedAt = dq.queuedAt;
            d.startedAt = java.time.Instant.now();
            debug.set(d);

            try {
                java.util.Map<String,Object> meta =
                        new com.fasterxml.jackson.databind.ObjectMapper().readValue(metaJson, java.util.Map.class);

                String projectKey = (String) meta.get("projectKey");
                Long memberId     = toLong(meta.get("memberId"));
                Long repositoryId = toLong(meta.get("repositoryId"));
                Long draftId      = toLong(meta.get("draftId"));
                Long diffId       = toLong(meta.get("diffId"));
                String checksum   = (String) meta.get("lastChecksum");

                // 경로로부터 분석 준비
                String dir = sonarService.extractAndPrepare(new File(zipPath), projectKey);

                // SONAR
                d.step = "SONAR"; debug.set(d);
                sonarService.runSonarScanner(dir, projectKey);

                // RESULT_DB
                d.step = "RESULT_DB"; debug.set(d);
                sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, checksum, projectKey);

                // CLEANUP
                d.step = "CLEANUP"; debug.set(d);
                sonarService.deleteProject(projectKey);

                d.status = "SUCCESS";
                d.step = "DONE";
                d.finishedAt = java.time.Instant.now();
                debug.set(d);
            } catch (Exception e) {
                DebugInfo df = new DebugInfo();
                df.jobId = jobId;
                df.status = "FAILED";
                df.step = "ERROR";
                df.queuedAt = dq.queuedAt;
                df.startedAt = d.startedAt;
                df.finishedAt = java.time.Instant.now();
                df.error = e.getMessage();
                debug.set(df);
            } finally {
                // 임시 zip 파일 삭제
                try { Files.deleteIfExists(Path.of(zipPath)); } catch (Exception ignore) {}
            }
        });

        return jobId;
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        return (v instanceof Number n) ? n.longValue() : Long.parseLong(v.toString());
    }
}
