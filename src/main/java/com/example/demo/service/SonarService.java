package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.Enumeration;
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

        // zip 저장
        File tempZip = File.createTempFile("upload-", ".zip");
        zipFile.transferTo(tempZip);
        unzip(tempZip, targetDir);

        String rootDir = targetDir.getAbsolutePath(); // 올바른 경로 설정

        // 동적으로 소스 디렉토리 설정
        String sourcePath = "src/main/java";
        if (!new File(rootDir, sourcePath).exists()) {
            if (new File(rootDir, "src").exists()) {
                sourcePath = "src";
            } else {
                throw new RuntimeException("소스 폴더를 찾을 수 없습니다.");
            }
        }
        // sonar-project.properties 생성
        File propFile = new File(targetDir, "sonar-project.properties");
        try (PrintWriter writer = new PrintWriter(propFile)) {
            writer.println("sonar.projectKey=" + projectKey);
            writer.println("sonar.projectName=" + projectKey);
            writer.println("sonar.sources=src/main/java");
            writer.println("sonar.java.binaries=target/classes");
            writer.println("sonar.java.source=17");
            writer.println("sonar.host.url=" + sonarHost);
            writer.println("sonar.token=" + sonarToken);
        }

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

    public void runSonarScanner(String dir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("sonar-scanner");
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
}