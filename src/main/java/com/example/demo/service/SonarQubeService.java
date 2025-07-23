//package com.example.demo.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.http.*;
//
//import java.io.*;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//public class SonarQubeService {
//
//    @Value("${sonarqube.host}")
//    private String sonarHost;
//
//    @Value("${sonarqube.token}")
//    private String sonarToken;
//
//    public String getAnalysisResult(String projectKey) throws InterruptedException {
//        System.out.println("소나 토큰 : " + sonarToken);
//
//        RestTemplate restTemplate = new RestTemplate();
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBasicAuth(sonarToken, "");
//        HttpEntity<String> entity = new HttpEntity<>(headers);
//
//        String statusUrl = sonarHost + "/api/ce/component?component=" + projectKey;
//        int maxRetries = 5;
//        int delayMillis = 2000;
//
//        // 1. 분석이 끝날 때까지 기다리기
//        for (int i = 0; i < maxRetries; i++) {
//            try {
//                ResponseEntity<String> response = restTemplate.exchange(statusUrl, HttpMethod.GET, entity, String.class);
//                String body = response.getBody();
//                if (body != null && body.contains("\"status\":\"SUCCESS\"")) {
//                    System.out.println("SonarQube 분석 완료 감지됨");
//                    break;
//                } else {
//                    System.out.println("분석 대기 중... " + (i + 1) + "/" + maxRetries);
//                    Thread.sleep(delayMillis);
//                }
//            } catch (Exception e) {
//                System.out.println("상태 확인 실패: " + e.getMessage());
//                Thread.sleep(delayMillis);
//            }
//        }
//
//        // 2. 실제 측정 결과 가져오기
//        String measuresUrl = sonarHost + "/api/measures/component?component=" + projectKey
//                + "&metricKeys=bugs,vulnerabilities,code_smells,coverage";
//
//        for (int i = 0; i < 3; i++) {
//            try {
//                ResponseEntity<String> response = restTemplate.exchange(measuresUrl, HttpMethod.GET, entity, String.class);
//                System.out.println("분석 결과 가져오기 성공");
//                return response.getBody();
//            } catch (HttpClientErrorException.NotFound e) {
//                System.out.println("분석 결과 대기 중... " + (i + 1) + "/3");
//                Thread.sleep(delayMillis);
//            }
//        }
//
//        throw new RuntimeException("분석 결과를 가져오지 못했습니다: " + projectKey);
//    }
//
//    public void deleteProject(String projectKey) {
//        try {
//            String sonarBaseUrl = "http://localhost:9000";
//            String deleteUrl = sonarBaseUrl + "/api/projects/delete?project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
//
//            String adminUsername = "admin";
//            String adminPassword = "teamprojectY1!";
//
//            HttpURLConnection connection = (HttpURLConnection) new URL(deleteUrl).openConnection();
//            connection.setRequestMethod("POST");
//            String basicAuth = "Basic " + Base64.getEncoder()
//                    .encodeToString((adminUsername + ":" + adminPassword).getBytes(StandardCharsets.UTF_8));
//            connection.setRequestProperty("Authorization", basicAuth);
//
//            int responseCode = connection.getResponseCode();
//            if (responseCode == 204) {
//                System.out.println(" 프로젝트 삭제 성공");
//            } else {
//                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//                String response = in.lines().collect(Collectors.joining());
//                in.close();
//                System.out.println(" 프로젝트 삭제 실패: " + response);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void deleteDirectoryRecursively(File dir) {
//        if (dir == null || !dir.exists()) return;
//
//        File[] files = dir.listFiles();
//        if (files != null) {
//            for (File file : files) {
//                if (file.isDirectory()) {
//                    deleteDirectoryRecursively(file);
//                } else {
//                    file.delete();
//                }
//            }
//        }
//        dir.delete();
//    }
//
//
//    private void createSonarPropertiesFile(File projectDir, String projectKey) throws IOException {
//        File propertiesFile = new File(projectDir, "sonar-project.properties");
//
//        // 자동으로 소스 경로 감지
//        String detectedSource = detectSourceFolder(projectDir);
//
//        try (PrintWriter writer = new PrintWriter(propertiesFile)) {
//            writer.println("sonar.projectKey=" + projectKey);
//            writer.println("sonar.projectName=" + projectKey);
//            writer.println("sonar.projectVersion=1.0");
//            writer.println("sonar.sources=" + detectedSource);
//
//            File sourceDir = new File(detectedSource);
//
//            if (containsExtension(sourceDir, ".java")) {
//                writer.println("sonar.java.binaries=" +
//                        (new File(projectDir, "target/classes").exists() ? "target/classes" : "build/classes/java/main"));
//                writer.println("sonar.java.source=17");
//            } else if (containsExtension(sourceDir, ".py")) {
//                writer.println("sonar.language=py");
//                writer.println("sonar.python.version=3.10");
//            } else if (containsExtension(sourceDir, ".js")) {
//                writer.println("sonar.language=js");
//            }
//
//            writer.println("sonar.login=" + sonarToken);
//        }
//    }
//    // 분석 대상 소스 경로 자동 탐색
//    private String detectSourceFolder(File baseDir) {
//        List<String> extensions = List.of(".js", ".py", ".java");
//        Queue<File> queue = new LinkedList<>();
//        queue.add(baseDir);
//
//        while (!queue.isEmpty()) {
//            File current = queue.poll();
//            File[] files = current.listFiles();
//            if (files == null) continue;
//
//            boolean hasSourceFile = Arrays.stream(files)
//                    .anyMatch(f -> extensions.stream().anyMatch(ext -> f.getName().endsWith(ext)));
//
//            if (hasSourceFile) {
//                return current.getAbsolutePath(); // ✅ 분석 소스 폴더로 사용
//            }
//
//            // 하위 디렉터리 탐색
//            Arrays.stream(files)
//                    .filter(File::isDirectory)
//                    .forEach(queue::add);
//        }
//
//        throw new RuntimeException("⚠️ 분석 가능한 소스 파일이 없습니다.");
//    }
//
//
//    private boolean containsExtension(File dir, String ext) {
//        if (!dir.exists() || !dir.isDirectory()) return false;
//        for (File file : dir.listFiles()) {
//            if (file.isDirectory()) {
//                if (containsExtension(file, ext)) return true;
//            } else if (file.getName().endsWith(ext)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//}