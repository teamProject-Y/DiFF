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

    // ✅ 스트리밍: 분석만, 끝날 때까지 무조건 기다림(heartbeat로 연결 유지)
    @PostMapping(value = "/analyze-stream", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<StreamingResponseBody> analyzeStream(@RequestBody Map<String, Object> req) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .body(out -> {
                    var writer = new java.io.OutputStreamWriter(out, java.nio.charset.StandardCharsets.UTF_8);

                    Path tempDir = null;
                    String key = null;
                    String projectKey = null;

                    // ❶ 5초 heartbeat로 프록시 유휴 타임아웃 방지
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
                        projectKey = "M-" + memberId + "_R-" + repositoryId + "_A-" + draftId + "_C-" + lastChecksum;

                        // 시작 신호 + heartbeat 시작
                        writer.write("START\n"); writer.flush();
                        timer.scheduleAtFixedRate(hb, 5_000, 5_000);

                        // ❷ 임시 디렉토리 + 다운로드/압축해제
                        tempDir = Files.createTempDirectory("diff_");
                        String extractedDir = r2Service.downloadAndUnzip(key, tempDir.toString());
                        writer.write("UNZIPPED\n"); writer.flush();

                        // ❸ Sonar 분석 (동기 실행)
                        sonarService.runSonarScanner(extractedDir, projectKey);
                        writer.write("SCANNED\n"); writer.flush();

                        // (선택) 환경에 따라 runSonarScanner가 비동기 트리거라면
                        // 여기서 SUCCESS까지 짧게 폴링하는 함수를 넣을 수 있음.
                        // waitUntilSuccess(projectKey, writer);

                        // ❹ 결과 조회 + DB 저장
                        String result = sonarService.getAnalysisResult(projectKey);
                        sonarService.analysisInsertDB(repositoryId, memberId, draftId, diffId, lastChecksum, projectKey);
                        writer.write("SAVED\n"); writer.flush();

                        // ❺ 최종 완료 신호 + 결과 본문
                        writer.write("DONE\n"); writer.flush();
                        writer.write(result + "\n"); writer.flush();

                    } catch (Exception e) {
                        writer.write(("ERROR " + e.getMessage() + "\n"));
                        writer.flush();
                    } finally {
                        // 정리는 마지막에 한 번만
                        timer.cancel();
                        try {
                            if (projectKey != null) {
                                try { sonarService.deleteProject(projectKey); } catch (Exception ignore) {}
                            }
                            if (key != null) {
                                try { r2Service.deleteFile(key); } catch (Exception ignore) {}
                            }
                            if (tempDir != null) {
                                try { FileUtils.deleteDirectory(tempDir.toFile()); } catch (Exception ignore) {}
                            }
                        } catch (Exception ignored) {}
                    }
                });
    }

    // 필요시: Sonar 작업 완료까지 대기하는 헬퍼(선택)
    // private void waitUntilSuccess(String projectKey, java.io.OutputStreamWriter writer) {
    //     long deadline = System.currentTimeMillis() + 10 * 60_000; // 최대 10분
    //     try {
    //         String status;
    //         do {
    //             status = sonarService.getTaskStatus(projectKey); // 직접 구현: QUEUED/RUNNING/SUCCESS/FAILED
    //             if ("FAILED".equals(status)) {
    //                 writer.write("ERROR Sonar analysis failed\n"); writer.flush();
    //                 throw new RuntimeException("Sonar FAILED");
    //             }
    //             if (!"SUCCESS".equals(status)) {
    //                 Thread.sleep(2000);
    //             }
    //         } while (!"SUCCESS".equals(status) && System.currentTimeMillis() < deadline);
    //         if (!"SUCCESS".equals(status)) {
    //             writer.write("ERROR Sonar analysis timeout\n"); writer.flush();
    //             throw new RuntimeException("Sonar TIMEOUT");
    //         }
    //     } catch (Exception e) {
    //         throw new RuntimeException(e);
    //     }
    // }

}
