package com.example.demo.controller;

import com.example.demo.service.SonarService;
import com.example.demo.vo.Rq;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class SonarUploadController {

    @Autowired
    private SonarService sonarService;

    @Autowired
    private Rq rq;

    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<String> uploadSource(@RequestParam("file") MultipartFile zipFile) {
        try {
            // 1. 사용자 및 커밋 기반 projectKey 생성
            Long memberId = rq.getLoginedMemberId();
            String commitId = UUID.randomUUID().toString();
            String projectKey = "temp_" + memberId + "_" + commitId;

            System.out.println("사용자 ID: " + memberId);
            System.out.println("생성된 Project Key: " + projectKey);

            // 2. 압축 해제 및 sonar-project.properties 생성
            String extractedPath = sonarService.extractAndPrepare(zipFile, projectKey);
            System.out.println("압축 해제 위치: " + extractedPath);

            // 3. 분석 실행
            sonarService.runSonarScanner(extractedPath,projectKey);

            // 4. 결과 조회
            String result = sonarService.getAnalysisResult(projectKey);
            System.out.println("분석 결과: " + result);

            grantProjectAdminPermission(projectKey); // 자동으로 admin 권한 부여
            Thread.sleep(2000);
            sonarService.deleteProject(projectKey);
            System.out.println("🧹 SonarQube 프로젝트 삭제 완료: " + projectKey);


            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("분석 중 오류 발생: " + e.getMessage());
        }

    }

    private void grantProjectAdminPermission(String projectKey) {
        String sonarBaseUrl = "http://localhost:9000";
        String apiEndpoint = sonarBaseUrl + "/api/permissions/add_user";

        String login = "admin"; // 권한을 부여할 사용자
        String password = "teamprojectY1!"; // admin 계정 비밀번호

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
                System.out.println("프로젝트 관리자 권한 부여 완료: " + projectKey);
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();
                System.out.println("권한 부여 실패: " + response);
            }

        } catch (IOException e) {
            System.out.println("권한 부여 중 예외 발생: " + e.getMessage());
        }
    }

}