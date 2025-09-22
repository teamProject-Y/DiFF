package com.example.demo.controller;

import com.example.demo.service.R2Service;
import com.example.demo.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/r2")
public class R2Controller {

    private final SonarService sonarService;

    private final R2Service r2Service;

    @PostMapping("/analyze")
    public ResponseEntity<String> analyzeFromR2(@RequestBody Map<String, Object> req) {
        System.out.println("📦 analyzeFromR2 req = " + req);
        Path tempDir = null;
        String key = null;

        try {
            Long memberId = ((Number) req.get("memberId")).longValue();
            Long repositoryId = ((Number) req.get("repositoryId")).longValue();
            Long draftId = ((Number) req.get("draftId")).longValue();
            Long diffId = ((Number) req.get("diffId")).longValue();
            String lastChecksum = (String) req.get("lastChecksum");
            key = (String) req.get("key");

            String projectKey = "M-" + memberId + "_R-" + repositoryId + "_A-" + draftId + "_C-" + lastChecksum;

            // 1. 임시 디렉토리 생성
            tempDir = Files.createTempDirectory("diff_");
            String extractedDir = r2Service.downloadAndUnzip(key, tempDir.toString());

            // 2. 분석 실행
            sonarService.runSonarScanner(extractedDir, projectKey);

            // 3. 결과 조회 + DB 저장
            String result = sonarService.getAnalysisResult(projectKey);
            sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, lastChecksum, projectKey);

            // 4. DB 저장 성공 후 → R2 파일 삭제
            try {
                r2Service.deleteFile(key);
                System.out.println("🗑️ R2 파일 삭제 완료: " + key);
            } catch (Exception ex) {
                System.err.println("⚠️ R2 파일 삭제 실패: " + key + " (" + ex.getMessage() + ")");
            }
            sonarService.deleteProject(projectKey);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ 분석 실패: " + e.getMessage());
        } finally {
            // 5. cleanup: 임시 디렉토리 삭제
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                    System.out.println("🗑️ 임시 디렉토리 삭제 완료: " + tempDir);
                } catch (IOException ex) {
                    System.err.println("⚠️ 임시 디렉토리 삭제 실패: " + ex.getMessage());
                }
            }
        }
    }

    @PostMapping(value = "/analyze-stream", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> analyzeStream(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok()
                // 프록시 버퍼링/캐시 방지 헤더 (가능하면 넣기)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(out -> {
                    var writer = new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8);

                    Path tempDir = null;
                    String key = null;

                    // 10초마다 점(.)을 흘려보내는 heartbeat
                    final var timer = new java.util.Timer(true);
                    final var hb = new java.util.TimerTask() {
                        @Override public void run() {
                            try { writer.write(".\n"); writer.flush(); } catch (Exception ignored) {}
                        }
                    };

                    try {
                        Long memberId     = ((Number) req.get("memberId")).longValue();
                        Long repositoryId = ((Number) req.get("repositoryId")).longValue();
                        Long draftId      = ((Number) req.get("draftId")).longValue();
                        Long diffId       = ((Number) req.get("diffId")).longValue();
                        String lastChecksum = (String) req.get("lastChecksum");
                        key = (String) req.get("key");
                        String projectKey = "M-" + memberId + "_R-" + repositoryId + "_A-" + draftId + "_C-" + lastChecksum;

                        // 시작 신호 + 즉시 flush
                        writer.write("START\n"); writer.flush();
                        timer.scheduleAtFixedRate(hb, 10_000, 10_000);

                        // 1) unzip
                        tempDir = java.nio.file.Files.createTempDirectory("diff_");
                        String extractedDir = r2Service.downloadAndUnzip(key, tempDir.toString());
                        writer.write("UNZIPPED\n"); writer.flush();

                        // 2) Sonar 분석
                        sonarService.runSonarScanner(extractedDir, projectKey);
                        writer.write("SCANNED\n"); writer.flush();

                        // 3) 결과 조회 + DB 저장
                        String result = sonarService.getAnalysisResult(projectKey);
                        sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, lastChecksum, projectKey);
                        writer.write("SAVED\n"); writer.flush();

                        // 4) 정리
                        try { r2Service.deleteFile(key); } catch (Exception ignore) {}
                        sonarService.deleteProject(projectKey);

                        // 완료
                        writer.write("DONE\n"); writer.flush();
                        writer.write(result + "\n"); writer.flush();
                    } catch (Exception e) {
                        writer.write(("ERROR " + e.getMessage() + "\n"));
                        writer.flush();
                    } finally {
                        timer.cancel();
                        if (tempDir != null) {
                            try { org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory(tempDir.toFile()); } catch (Exception ignore) {}
                        }
                    }
                });
    }

}
