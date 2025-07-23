package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class SonarService {

    @Value("${sonarqube.host}")
    private String sonarHost;

    @Value("${sonarqube.token}")
    private String sonarToken;

    public String extractAndPrepare(MultipartFile zipFile, String projectKey) throws IOException {
        Path tempDir = Files.createTempDirectory("source-");
        File targetDir = tempDir.toFile();

        // zip 저장 및 압축 해제
        File tempZip = File.createTempFile("upload-", ".zip");
        zipFile.transferTo(tempZip);
        unzip(tempZip, targetDir);

        // sonar-project.properties 자동 생성
        createSonarPropertiesFile(targetDir, projectKey);

        return targetDir.getAbsolutePath();
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File newFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    newFile.getParentFile().mkdirs();
                    try (InputStream is = zip.getInputStream(entry);
                         FileOutputStream fos = new FileOutputStream(newFile)) {
                        is.transferTo(fos);
                    }
                }
            }
        }
    }

    public void runSonarScanner(String dir, String projectKey) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "sonar-scanner",
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.host.url=" + sonarHost,
                "-Dsonar.login=" + sonarToken
        );
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);
        Process process = pb.start();


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("▶ " + line);
            }
        }

        process.waitFor();
    }

    public String getAnalysisResult(String projectKey) throws InterruptedException {
        System.out.println("getAnalysisResult : 소나 토큰 : " + sonarToken);

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sonarToken, "");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String statusUrl = sonarHost + "/api/ce/component?component=" + projectKey;
        int maxRetries = 5;
        int delayMillis = 2000;

        // 1. 분석이 끝날 때까지 기다리기
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(statusUrl, HttpMethod.GET, entity, String.class);
                String body = response.getBody();
                if (body != null && body.contains("\"status\":\"SUCCESS\"")) {
                    System.out.println("SonarQube 분석 완료 감지됨");
                    break;
                } else {
                    System.out.println("분석 대기 중... " + (i + 1) + "/" + maxRetries);
                    Thread.sleep(delayMillis);
                }
            } catch (Exception e) {
                System.out.println("상태 확인 실패: " + e.getMessage());
                Thread.sleep(delayMillis);
            }
        }

        // 2. 실제 측정 결과 가져오기
        String measuresUrl = sonarHost + "/api/measures/component?component=" + projectKey
                + "&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,complexity";
        System.out.println("measuresUrl : " + measuresUrl);
        for (int i = 0; i < 10; i++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(measuresUrl, HttpMethod.GET, entity, String.class);
                System.out.println("분석 결과 가져오기 성공");
                return response.getBody();
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("분석 결과 대기 중... " + (i + 1) + "/10");
                Thread.sleep(delayMillis);
            }
        }

        throw new RuntimeException("분석 결과를 가져오지 못했습니다: " + projectKey);
    }

    public void deleteProject(String projectKey) {
        try {
            String sonarBaseUrl = "http://localhost:9000";
            String deleteUrl = sonarBaseUrl + "/api/projects/delete?project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);

            String adminUsername = "admin";
            String adminPassword = "teamprojectY1!";

            HttpURLConnection connection = (HttpURLConnection) new URL(deleteUrl).openConnection();
            connection.setRequestMethod("POST");
            String basicAuth = "Basic " + Base64.getEncoder()
                    .encodeToString((adminUsername + ":" + adminPassword).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", basicAuth);

            int responseCode = connection.getResponseCode();
            if (responseCode == 204) {
                System.out.println(" 프로젝트 삭제 성공");
            } else {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String response = in.lines().collect(Collectors.joining());
                in.close();
                System.out.println(" 프로젝트 삭제 실패: " + response);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectoryRecursively(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }


    private void createSonarPropertiesFile(File projectDir, String projectKey) throws IOException {
        File propertiesFile = new File(projectDir, "sonar-project.properties");
        System.out.println(propertiesFile.getAbsolutePath() + " 소스 경로 감지 ");

        String javaSourcePath = findJavaSourceFolder(projectDir);
        String classPath = findClassFolder(projectDir);
        String mainSourcePath = detectSourceFolder(projectDir);

        System.out.println("✅ [DEBUG] .java 포함 여부: " + (javaSourcePath != null));
        System.out.println("✅ [DEBUG] .class 경로 존재 여부: " + (classPath != null));
        System.out.println("✅ [DEBUG] .js 포함 여부: " + containsExtension(new File(mainSourcePath), ".js"));
        System.out.println("✅ [DEBUG] .py 포함 여부: " + containsExtension(new File(mainSourcePath), ".py"));

        try (PrintWriter writer = new PrintWriter(propertiesFile)) {
            writer.println("sonar.projectKey=" + projectKey);
            writer.println("sonar.projectName=" + projectKey);
            writer.println("sonar.projectVersion=1.0");
            writer.println("sonar.host.url=" + sonarHost);

            // ✅ 1. sources 경로 지정
            List<String> sourcePaths = new ArrayList<>();
            if (javaSourcePath != null) {
                sourcePaths.add(projectDir.toPath().relativize(Path.of(javaSourcePath)).toString());
            }
            if (classPath != null && !sourcePaths.contains(projectDir.toPath().relativize(Path.of(classPath)).toString())) {
                sourcePaths.add(projectDir.toPath().relativize(Path.of(classPath)).toString());
            }
            if (sourcePaths.isEmpty()) {
                sourcePaths.add(projectDir.toPath().relativize(Path.of(mainSourcePath)).toString());
            }
            writer.println("sonar.sources=" + String.join(",", sourcePaths));

            // ✅ 2. Java 설정
            if (javaSourcePath != null && classPath != null) {
                String classPathRel = projectDir.toPath().relativize(Path.of(classPath)).toString();
                writer.println("sonar.java.binaries=" + classPathRel);
                writer.println("sonar.java.source=17");
            }

            // ✅ 3. Python 설정 (자동 감지)
            if (containsExtension(new File(mainSourcePath), ".py")) {
                writer.println("sonar.language=py");
                writer.println("sonar.python.version=3.10");
            }

            // ✅ 4. JS 설정 (자동 감지) – 필요 없을 수도 있음 (JS 플러그인 자동 인식)
            if (containsExtension(new File(mainSourcePath), ".js")) {
                // JS는 보통 language를 안 넣어도 됨. 혹시 넣고 싶으면 아래 주석 해제
                // writer.println("sonar.language=js");
            }

            writer.println("sonar.login=" + sonarToken);
        }
    }

    private String findClassFolder(File projectDir) {
        File[] classDirs = {
                new File(projectDir, "target/classes"),
                new File(projectDir, "build/classes/java/main")
        };

        for (File dir : classDirs) {
            if (dir.exists() && dir.isDirectory()) {
                return dir.getAbsolutePath();
            }
        }

        return null;
    }


    private String findJavaSourceFolder(File projectDir) {
        return findDirectoryContainingExtension(projectDir, ".java");
    }

    private String findDirectoryContainingExtension(File dir, String extension) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        boolean containsTargetFile = false;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(extension)) {
                containsTargetFile = true;
            }
        }
        if (containsTargetFile) {
            return dir.getAbsolutePath();
        }

        for (File file : files) {
            if (file.isDirectory()) {
                String found = findDirectoryContainingExtension(file, extension);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // 분석 대상 소스 경로 자동 탐색
    private String detectSourceFolder(File baseDir) {
        File targetClasses = new File(baseDir, "target/classes");
        if (targetClasses.exists() && targetClasses.isDirectory()) {
            return targetClasses.getAbsolutePath();
        }
        throw new RuntimeException("⚠️ target/classes 디렉터리가 없습니다.");
    }
    private boolean containsExtension(File dir, String ext) {
        if (!dir.exists() || !dir.isDirectory()) return false;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                if (containsExtension(file, ext)) return true;
            } else if (file.getName().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}