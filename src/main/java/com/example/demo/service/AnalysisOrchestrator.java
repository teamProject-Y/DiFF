package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public String enqueue(MultipartFile zip, String metaJson) {
        String jobId = UUID.randomUUID().toString();

        // 디버깅용
        lastJobId.set(jobId);
        var dq = new DebugInfo();
        dq.jobId = jobId;
        dq.status = "QUEUED";
        dq.step = "QUEUED";
        dq.queuedAt = Instant.now();
        debug.set(dq);

        analysisExecutor.submit(() -> {
            try {
                // RUNNING 시작
                var dr = new DebugInfo();
                dr.jobId = jobId;
                dr.status = "RUNNING";
                dr.step = "EXTRACT";
                dr.queuedAt = dq.queuedAt;
                dr.startedAt = Instant.now();
                debug.set(dr);

                Map<String,Object> meta = objectMapper.readValue(metaJson, Map.class);
                String projectKey = (String) meta.get("projectKey");
                Long memberId     = toLong(meta.get("memberId"));
                Long repositoryId = toLong(meta.get("repositoryId"));
                Long draftId      = toLong(meta.get("draftId"));
                Long diffId       = toLong(meta.get("diffId"));
                String checksum   = (String) meta.get("lastChecksum");

                // EXTRACT
                String dir = sonarService.extractAndPrepare(zip, projectKey);

                // SONAR
                dr.step = "SONAR";
                debug.set(dr);
                sonarService.runSonarScanner(dir, projectKey);

                // RESULT_DB
                dr.step = "RESULT_DB";
                debug.set(dr);
                sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, checksum, projectKey);

                // CLEANUP
                dr.step = "CLEANUP";
                debug.set(dr);
                sonarService.deleteProject(projectKey);

                // SUCCESS
                dr.status = "SUCCESS";
                dr.step = "DONE";
                dr.finishedAt = Instant.now();
                debug.set(dr);
            } catch (Exception e) {
                var df = new DebugInfo();
                df.jobId = jobId;
                df.status = "FAILED";
                df.step = "ERROR";
                df.queuedAt = dq.queuedAt;
                df.startedAt = dq.startedAt;
                df.finishedAt = Instant.now();
                df.error = e.getMessage();
                debug.set(df);
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
