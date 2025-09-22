package com.example.demo.controller;

import com.example.demo.service.R2Service;
import com.example.demo.service.SonarService;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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


}
