package com.example.demo.service;

import com.example.demo.repository.AnalysisRepository;
import com.example.demo.repository.DraftRepository;
import com.example.demo.vo.Analysis;
import com.example.demo.vo.AnalysisLanguage;
import com.example.demo.vo.Draft;
import com.example.util.Ut;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private DraftRepository draftRepository;
    @Autowired
    private R2Service r2Service;

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

    /**
     * sonar-scanner 실행
     */
    public void runSonarScanner(String dir, String projectKey) throws IOException, InterruptedException {
        System.out.println("🛰️ runSonarScanner in: " + dir);
        System.out.println("🛰️ sonarHost=" + sonarHost + ", token.len=" + (sonarToken == null ? 0 : sonarToken.length()));

        // 1. sonar-project.properties 자동 생성
        createSonarPropertiesFile(new File(dir), projectKey);

        // 2. sonar-scanner 실행
        ProcessBuilder pb = new ProcessBuilder(
                "sonar-scanner",
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.host.url=" + sonarHost,
                "-Dsonar.token=" + sonarToken
        );
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 3. 로그 출력
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("▶ [scanner] " + line);
            }
        }

        // 4. 종료 코드 확인
        int exit = process.waitFor();
        System.out.println("🛰️ sonar-scanner exitCode=" + exit);

        if (exit != 0) {
            throw new RuntimeException("sonar-scanner failed. exit=" + exit);
        }
    }


    /**
     * SonarQube API에서 분석 결과 조회
     */
    public String getAnalysisResult(String projectKey) throws InterruptedException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sonarToken, "");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String statusUrl = sonarHost + "/api/ce/component?component=" + projectKey;
        int maxRetries = 10;
        int delayMillis = 3000;

        // SUCCESS 나올 때까지 대기
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(statusUrl, HttpMethod.GET, entity, String.class);
                if (response.getBody() != null && response.getBody().contains("\"status\":\"SUCCESS\"")) {
                    break;
                }
                Thread.sleep(delayMillis);
            } catch (Exception e) {
                Thread.sleep(delayMillis);
            }
        }

        // 실제 결과 요청
        String measuresUrl = sonarHost + "/api/measures/component?component=" + projectKey
                + "&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,complexity,ncloc_language_distribution";

        for (int i = 0; i < 10; i++) {
            try {
                ResponseEntity<String> response = restTemplate.exchange(measuresUrl, HttpMethod.GET, entity, String.class);
                System.out.println("분석 결과 가져오기 성공");
                return response.getBody();
            } catch (HttpClientErrorException.NotFound e) {
                Thread.sleep(delayMillis);
            }
        }
        throw new RuntimeException("❌ SonarQube 결과 못 가져옴: " + projectKey);
    }

    /**
     * 분석 결과 DB 저장
     */
    public void analysisInsertDB(Long repositoryId, Long memberId, Long draftId, Long diffId,
                                 String checksum, String projectKey) throws Exception {
        String resultJson = getAnalysisResult(projectKey);
        if (resultJson == null || !resultJson.trim().startsWith("{")) return;

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(resultJson).get("component");
        String projectKeyFromJson = root.get("key").asText();
        String projectName = root.get("name").asText();
        JsonNode measures = root.get("measures");

        Map<String, String> metricMap = new HashMap<>();
        for (JsonNode measure : measures) {
            metricMap.put(measure.get("metric").asText(), measure.get("value").asText());
        }

        Draft draft = draftRepository.getDraftById(draftId);
        if (draft == null) return;
        if (checksum == null) checksum = draft.getChecksum();

        Analysis analysis = Analysis.builder()
                .repositoryId(repositoryId)
                .memberId(memberId)
                .articleId(draftId)
                .diffId(diffId)
                .checksum(checksum)
                .projectKey(projectKeyFromJson)
                .projectName(projectName)
                .coverage(Ut.parseDoubleOrZero(metricMap.get("coverage")))
                .bugs(Ut.parseIntOrZero(metricMap.get("bugs")))
                .complexity(Ut.parseIntOrZero(metricMap.get("complexity")))
                .codeSmells(Ut.parseIntOrZero(metricMap.get("code_smells")))
                .duplicatedLinesDensity(Ut.parseDoubleOrZero(metricMap.get("duplicated_lines_density")))
                .vulnerabilities(Ut.parseIntOrZero(metricMap.get("vulnerabilities")))
                .build();

        // 각 항목 점수 저장
        analysisRepository.insert(analysis);
        Long analyzeId = analysis.getId();
        System.out.println("✅ 분석 결과 저장 완료 - analyzeId: " + analyzeId);

        // 총점 저장
        analysis.setId(analyzeId);
        analysisRepository.updateTotalScore(analysis);
        System.out.println("✅ 총점 계산 완료 - analyzeId: " + analysis.getTotalScore());

        // 언어 분포 저장
        String langRaw = metricMap.get("ncloc_language_distribution");

        if (langRaw != null) {
            List<AnalysisLanguage> langs = parseLanguageDistribution(langRaw, analyzeId);
            langs.forEach(analysisRepository::insertLanguage);
        }
        System.out.println("✅ 언어 분포 저장 완료 - " + langRaw + "개 언어");
    }

    public List<AnalysisLanguage> parseLanguageDistribution(String raw, Long analyzeId) {
        System.out.println("parseLanguageDistribution 잔입 raw: " + raw);
        List<AnalysisLanguage> result = new ArrayList<>();
        String[] pairs = raw.split(";");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                String lang = parts[0].trim();
                int lines = Integer.parseInt(parts[1].trim());
                result.add(AnalysisLanguage.builder()
                        .analyzeId(analyzeId)
                        .language(lang)
                        .lines(lines)
                        .build());
            }
        }
        return result;
    }

    public void deleteProject(String projectKey) {
        try {
            String sonarBaseUrl = "https://sonar.diff.io.kr";
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

    private void createSonarPropertiesFile(File projectDir, String projectKey) throws IOException {
        File propertiesFile = new File(projectDir, "sonar-project.properties");
        System.out.println(propertiesFile.getAbsolutePath() + " 소스 경로 감지 ");

        // 여러 유효 폴더 모두 포함 (하드코딩 제거, 외부 빌드/라이브러리 폴더 제외)
        List<String> sourcePaths = detectAllValidSourceFolders(projectDir).stream()
                .map(path -> projectDir.toPath().relativize(Path.of(path)).toString())
                .distinct()
                .collect(Collectors.toList());

        // 멀티모듈 지원: 모든 모듈의 .class 경로 찾기
        List<String> javaBins = findAllJavaBinaries(projectDir).stream()
                .map(path -> projectDir.toPath().relativize(Path.of(path)).toString())
                .distinct()
                .collect(Collectors.toList());

        // 언어 포함 여부 체크
        boolean containsJava = !javaBins.isEmpty();
        boolean containsJS = containsExtension(projectDir, ".js");
        boolean containsPY = containsExtension(projectDir, ".py");

        System.out.println("✅ [DEBUG] .java 포함 여부: " + containsJava);
        System.out.println("✅ [DEBUG] .class 경로 개수: " + javaBins.size());
        System.out.println("✅ [DEBUG] .js 포함 여부: " + containsJS);
        System.out.println("✅ [DEBUG] .py 포함 여부: " + containsPY);

        try (PrintWriter writer = new PrintWriter(propertiesFile)) {
            writer.println("sonar.projectKey=" + projectKey);
            writer.println("sonar.projectName=" + projectKey);
            writer.println("sonar.projectVersion=1.0");
            writer.println("sonar.host.url=" + sonarHost);

            // 1. 소스 경로
            writer.println("sonar.sources=" + String.join(",", sourcePaths));

            // 2. 공통 제외 패턴 (외부 라이브러리/빌드 산출물)
            writer.println("sonar.exclusions=" + String.join(",",
                    "**/node_modules/**","**/build/**","**/dist/**","**/target/**",
                    "**/.venv/**","**/venv/**","**/.tox/**","**/.pytest_cache/**",
                    "**/.next/**","**/.nuxt/**","**/.yarn/**","**/.pnpm-store/**"));

            // 3. 포함 패턴 (원본 코드 확장자)
            writer.println("sonar.inclusions=" + String.join(",",
                    "**/*.java","**/*.kt","**/*.kts","**/*.py","**/*.js","**/*.jsx","**/*.ts","**/*.tsx"));

            // 4. Java 설정 (멀티모듈 binaries)
            if (containsJava) {
                writer.println("sonar.java.binaries=" + String.join(",", javaBins));
                writer.println("sonar.java.source=17");
            }

            // 5. Python 설정 (자동 감지)
            if (containsPY) {
                writer.println("sonar.python.version=3.10");
                // sonar.language는 지정 안 하면 JS/Java/Python 다 잡힘
            }

            writer.println("sonar.token=" + sonarToken);
        }

        System.out.println(" 최종 분석 대상 폴더들: " + sourcePaths);
        if (containsJava) {
            System.out.println(" Java 바이너리 경로들: " + javaBins);
        }
    }

    private List<String> findAllJavaBinaries(File root) {
        List<String> bins = new ArrayList<>();
        Deque<File> dq = new ArrayDeque<>();
        dq.add(root);

        while (!dq.isEmpty()) {
            File dir = dq.pollFirst();
            File[] list = dir.listFiles();
            if (list == null) continue;

            boolean isModuleRoot =
                    new File(dir, "pom.xml").isFile() ||
                            new File(dir, "build.gradle").isFile() ||
                            new File(dir, "build.gradle.kts").isFile() ||
                            new File(dir, "src/main/java").isDirectory() ||
                            new File(dir, "src/main/kotlin").isDirectory();

            if (isModuleRoot) {
                String[] candidates = {
                        "target/classes","target/test-classes",
                        "build/classes/java/main","build/classes/java/test",
                        "build/classes/kotlin/main","build/classes/kotlin/test"
                };
                for (String rel : candidates) {
                    File c = new File(dir, rel);
                    if (c.isDirectory()) bins.add(c.getAbsolutePath());
                }
            }

            for (File f : list) {
                String name = f.getName();
                if (f.isDirectory() &&
                        !name.equals(".git") && !name.equals(".idea") && !name.equals(".gradle") &&
                        !name.equals("node_modules") && !name.equals("build") && !name.equals("dist") &&
                        !name.equals("target") && !name.equals(".venv") && !name.equals("venv")) {
                    dq.add(f);
                }
            }
        }
        return bins.stream().distinct().collect(Collectors.toList());
    }

    private List<String> detectAllValidSourceFolders(File baseDir) {
        String[] candidates = {"src", "client", "apps", "js", "python", "."};
        List<String> validPaths = new ArrayList<>();

        for (String name : candidates) {
            File dir = new File(baseDir, name);
            System.out.println(" 후보 탐색 중: " + dir.getAbsolutePath());
            if (dir.exists() && dir.isDirectory()) {
                System.out.println(" 후보 선택됨: " + dir.getAbsolutePath());
                validPaths.add(dir.getAbsolutePath());
            }
        }

        // 아무 폴더도 없으면 루트 fallback
        if (validPaths.isEmpty()) {
            System.err.println("후보 중 유효한 폴더 없음. 루트로 fallback");
            validPaths.add(baseDir.getAbsolutePath());
        }

        return validPaths;
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