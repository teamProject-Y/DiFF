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
        final String scannerAbs = "/opt/sonar-scanner-5.0.1.3006-linux/bin/sonar-scanner";

        // ── 0) 기본 환경 로그
        System.out.println("=== [SONAR DIAG] START ===============================");
        System.out.println("user.name            = " + System.getProperty("user.name"));
        System.out.println("java.version         = " + System.getProperty("java.version"));
        System.out.println("java.home            = " + System.getProperty("java.home"));
        System.out.println("os.name              = " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("WORK DIR (arg)       = " + dir);
        System.out.println("sonar.host.url       = " + sonarHost);
        System.out.println("token.len            = " + (sonarToken == null ? 0 : sonarToken.length()));

        // ── 1) 실행 파일/쉘/작업 디렉토리 점검
        File scanner = new File(scannerAbs);
        System.out.println("scanner.exists       = " + scanner.exists());
        System.out.println("scanner.canExecute   = " + scanner.canExecute());
        System.out.println("scanner.path         = " + scanner.getAbsolutePath());

        File sh = new File("/bin/sh");
        System.out.println("/bin/sh exists       = " + sh.exists() + ", canExec=" + sh.canExecute());

        File wd = new File(dir);
        System.out.println("wd.exists            = " + wd.exists() + ", isDir=" + wd.isDirectory());
        if (wd.exists()) {
            String[] list = wd.list();
            System.out.println("wd.list sample       = " + (list == null ? "null" :
                    java.util.Arrays.stream(list).limit(10).reduce((a,b) -> a + ", " + b).orElse("(empty)")));
        }

        // sonar-scanner 스크립트의 shebang 확인 (첫 줄)
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(scanner))) {
            String shebang = br.readLine();
            System.out.println("scanner shebang      = " + shebang);
        } catch (Exception ignore) {
            System.out.println("scanner shebang      = <read fail>");
        }

        // which 확인(시스템 PATH 상 위치)
        runQuick("which sonar-scanner");

        // 링크/권한 자세히
        runQuick("ls -l " + scannerAbs);
        runQuick("ls -l /usr/local/bin/sonar-scanner");
        runQuick("readlink -f /usr/local/bin/sonar-scanner");

        // ── 2) 실행 준비
        String cmdStr = String.join(" ",
                scannerAbs,
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.host.url=" + sonarHost,
                "-Dsonar.token=" + sonarToken
        );

        ProcessBuilder pb = new ProcessBuilder(
                "/bin/bash", "-lc",
                "sonar-scanner " +
                        "-Dsonar.projectKey=" + projectKey + " " +
                        "-Dsonar.host.url="   + sonarHost  + " " +
                        "-Dsonar.token="      + sonarToken
        );
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);

// PATH에 스캐너 위치가 들어가도록 (안전빵)
        pb.environment().put("PATH",
                "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/sonar-scanner-" +
                        System.getenv().getOrDefault("SONAR_SCANNER_VERSION","5.0.1.3006") + "-linux/bin"
        );


        // PATH 보정
        java.util.Map<String, String> env = pb.environment();
        String newPath = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/sonar-scanner-5.0.1.3006-linux/bin";
        env.put("PATH", newPath);

        System.out.println("▶ exec CWD  = " + pb.directory().getAbsolutePath());
        System.out.println("▶ exec CMD  = " + cmdStr);
        System.out.println("▶ exec PATH = " + env.get("PATH"));

        // ── 3) 실행 + 출력 수집
        Process process = null;
        try {
            process = pb.start();
        } catch (IOException ioe) {
            System.out.println("❌ pb.start() IOException: " + ioe.getMessage());
            System.out.println("   HINT: error=2(ENOENT)이면 보통 해석기/경로 이슈. /bin/bash -lc 경유 실행을 시도하세요.");
            // 디버깅 대안: bash 경유로 재시도 (주석 해제해 테스트)
            // return runViaBash(dir, projectKey);

            throw ioe; // 현재는 그대로 던짐
        }

        try (java.io.BufferedReader reader =
                     new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("▶ [scanner] " + line);
            }
        }

        int exit = process.waitFor();
        System.out.println("🛰️ sonar-scanner exitCode = " + exit);
        System.out.println("=== [SONAR DIAG] END =================================");

        if (exit != 0) {
            throw new RuntimeException("❌ sonar-scanner failed. exit=" + exit);
        }
    }

    /** 간단 명령 실행해서 결과 보여주는 유틸 (디버깅용) */
    private void runQuick(String cmd) {
        try {
            Process p = new ProcessBuilder("/bin/bash", "-lc", cmd)
                    .redirectErrorStream(true).start();
            try (java.io.BufferedReader r =
                         new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                String line;
                System.out.println("[$] " + cmd);
                while ((line = r.readLine()) != null) System.out.println("    " + line);
            }
            p.waitFor();
        } catch (Exception e) {
            System.out.println("[$] " + cmd + " -> fail: " + e.getMessage());
        }
    }

    /** 필요시 bash 경유 재시도 버전 (원하면 주석 해제해서 사용) */
    @SuppressWarnings("unused")
    private boolean runViaBash(String dir, String projectKey) throws IOException, InterruptedException {
        String cmd = String.join(" ",
                "sonar-scanner",
                "-Dsonar.projectKey=" + projectKey,
                "-Dsonar.host.url=" + sonarHost,
                "-Dsonar.token=" + sonarToken
        );
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc", cmd);
        pb.directory(new File(dir));
        pb.redirectErrorStream(true);
        pb.environment().put("PATH",
                "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/sonar-scanner-5.0.1.3006-linux/bin");

        System.out.println("▶ [bash retry] CMD=" + String.join(" ", pb.command()));
        Process p = pb.start();
        try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
            for (String line; (line = r.readLine()) != null; ) System.out.println("▶ [bash] " + line);
        }
        int exit = p.waitFor();
        System.out.println("🛰️ [bash retry] exitCode=" + exit);
        return exit == 0;
    }


    public String getAnalysisResult(String projectKey) throws InterruptedException {
        System.out.println("getAnalysisResult sonar token : " + sonarToken);

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
                + "&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,complexity,ncloc_language_distribution";
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

    public void analysisInsertDB(Long repositoryId,
                                 Long memberId,
                                 Long draftId,
                                 Long diffId,
                                 String checksum,
                                 String projectKey ) throws IOException, InterruptedException {
        try {
            // 분석 결과 가져오기
            String resultJson = getAnalysisResult(projectKey);

            if (resultJson == null || !resultJson.trim().startsWith("{")) {
                System.out.println("❌ 분석 결과 JSON 아님! resultJson = " + resultJson);
                return;
            }

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(resultJson);
            JsonNode component = root.get("component");

            String projectKeyFromJson = component.get("key").asText();
            String projectName = component.get("name").asText();
            JsonNode measures = component.get("measures");

            Map<String, String> metricMap = new HashMap<>();
            for (JsonNode measure : measures) {
                metricMap.put(measure.get("metric").asText(), measure.get("value").asText());
            }

            // Draft 조회 (안전 확인용)
            Draft draft = draftRepository.getDraftById(draftId);
            if (draft == null) {
                System.out.println("❌ draftId=" + draftId + " 에 해당 Draft 없음!");
                return;
            }

            if (checksum == null) {
                checksum = draft.getChecksum();
            }
            System.out.println("🔑 draftId=" + draftId + " 의 checksum=" + checksum);

            // Analysis 저장
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

            analysisRepository.insert(analysis);
            Long analyzeId = analysis.getId();
            System.out.println("✅ 분석 결과 저장 완료 - analyzeId: " + analyzeId);

            // 언어 분포 저장
            String langRaw = metricMap.get("ncloc_language_distribution");
            if (langRaw != null) {
                List<AnalysisLanguage> languages = parseLanguageDistribution(langRaw, analyzeId);
                for (AnalysisLanguage lang : languages) {
                    analysisRepository.insertLanguage(lang);
                }
                System.out.println("✅ 언어 분포 저장 완료 - " + languages.size() + "개 언어");
            }

        } catch (Exception e) {
            System.out.println("❌ analysisInsertDB 분석 결과 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
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

    private Double parseDouble(String value) {
        try { return value == null ? null : Double.parseDouble(value); }
        catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String value) {
        try { return value == null ? null : Integer.parseInt(value); }
        catch (NumberFormatException e) { return null; }
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