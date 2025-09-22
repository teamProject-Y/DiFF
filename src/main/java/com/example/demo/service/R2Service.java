package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


import java.io.*;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class R2Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    /**
     * 파일 다운로드 후 로컬에 저장
     */
    public File downloadFile(String key, String localPath) {
        Path path = Paths.get(localPath);
        try {
            s3Client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build(),
                    path
            );
            return path.toFile();
        } catch (Exception e) {
            throw new RuntimeException("❌ 다운로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 다운로드 후 ZIP 압축 해제
     */
    public String downloadAndUnzip(String key, String targetDir) {
        try {
            Path zipPath = Paths.get(targetDir, key.replace("/", "_"));
            System.out.println("⬇️ R2 다운로드: " + key + " → " + zipPath);

            // 1. 다운로드
            downloadFile(key, zipPath.toString());

            // 2. unzip
            Path extractPath = Paths.get(targetDir, "unzipped");
            Files.createDirectories(extractPath);

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path newFilePath = extractPath.resolve(entry.getName());

                    if (entry.isDirectory()) {
                        Files.createDirectories(newFilePath);
                    } else {
                        Files.createDirectories(newFilePath.getParent());
                        try (OutputStream os = Files.newOutputStream(newFilePath)) {
                            zis.transferTo(os);
                        }
                    }
                    zis.closeEntry();
                }
            }

            System.out.println("📂 압축 해제 완료: " + extractPath);
            return extractPath.toString();

        } catch (Exception e) {
            throw new RuntimeException("❌ 다운로드/압축 해제 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}
