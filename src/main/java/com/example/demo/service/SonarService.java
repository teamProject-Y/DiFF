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
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private AnalysisService analysisService;

    @Value("${sonar.host}")
    private String sonarHost;

    @Value("${sonar.token}")
    private String sonarToken;

    @Value("${sonar.organization}")
    private String sonarOrg;

    public String extractAndPrepare(MultipartFile zipFile, String projectKey) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("source-");
        File targetDir = tempDir.toFile();

        // zip 저장 및 압축 해제
        File tempZip = File.createTempFile("upload-", ".zip");
        zipFile.transferTo(tempZip);
        unzip(tempZip, targetDir);

        // GitHub zipball wrapper 디렉토리 보정
        File[] children = targetDir.listFiles(File::isDirectory);
        if (children != null && children.length == 1) {
            File wrapper = children[0];
            if (wrapper.getName().matches(".+-[0-9a-f]{5,}.*")) {
                System.out.println("⚠️ GitHub zipball wrapper 감지 → baseDir 교체: " + wrapper.getAbsolutePath());
                targetDir = wrapper;
            }
        }

        // 빌드 실행 (target/classes 생성 목적)
        runBuild(targetDir);

        // sonar-project.properties 자동 생성
        createSonarPropertiesFile(targetDir, projectKey);

        return targetDir.getAbsolutePath();
    }

    private File findProjectRoot(File start, int maxDepth) {
        if (start == null || !start.isDirectory()) return null;
        Deque<File> q = new ArrayDeque<>();
        q.add(start);
        int depth = 0;
        while (!q.isEmpty() && depth <= maxDepth) {
            int sz = q.size();
            for (int i = 0; i < sz; i++) {
                File cur = q.poll();
                if (cur == null) continue;
                if (new File(cur, "pom.xml").exists()
                        || new File(cur, "build.gradle").exists()
                        || new File(cur, "build.gradle.kts").exists()) {
                    return cur;
                }
                File[] children = cur.listFiles(File::isDirectory);
                if (children != null) for (File c : children) q.add(c);
            }
            depth++;
        }
        return null;
    }

    private void runBuild(File dir) throws IOException, InterruptedException {
        // 0) zip 최상위 디렉토리가 루트가 아닐 수 있으니, 하위에서 pom/gradle 파일 자동 탐색
        File root = findProjectRoot(dir, 3); // 하위 3단계까지
        if (root == null) {
            System.out.println("⚠️ Maven/Gradle 프로젝트 아님. 빌드 스킵 (pom.xml/gradle 파일 없음)");
            return;
        }

        File pom = new File(root, "pom.xml");
        File gradle = new File(root, "build.gradle");
        File gradleKts = new File(root, "build.gradle.kts");
        File mvnw = new File(root, "mvnw");
        File gradlew = new File(root, "gradlew");

        if (pom.exists()) {
            System.out.println("📄 사용되는 pom.xml 경로: " + pom.getAbsolutePath());
        }

        // 1) 프리빌트 감지: 이미 classes가 있으면 스킵 (업로드가 산출물 포함시)
        if (new File(root, "target/classes").exists() || new File(root, "build/classes/java/main").exists()) {
            System.out.println("🔎 Prebuilt artifacts detected → build step skip (" + root.getAbsolutePath() + ")");
            return;
        }

        // 2) 래퍼 권한 보정
        chmodX(mvnw);
        chmodX(gradlew);

        // 3) 커맨드 구성(래퍼 우선)
        String cmd;
        if (pom.exists()) {
            String mvnCmd = mvnw.exists() ? "./mvnw" : "mvn";
            // -B 배치, 테스트 완전 스킵(둘 다 넣어 확실하게)
            cmd = mvnCmd + " -B -e -DskipTests -Dmaven.test.skip=true clean package";
        } else if (gradle.exists() || gradleKts.exists()) {
            String g = gradlew.exists() ? "./gradlew" : "gradle";
            cmd = g + " assemble --no-daemon --console=plain -x test";
        } else {
            System.out.println("⚠️ Maven/Gradle 프로젝트 아님. 빌드 스킵");
            return;
        }

        System.out.println("▶ 실행할 빌드 명령어: " + cmd);
        System.out.println("▶ 작업 디렉터리: " + root.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc", cmd);
        pb.directory(root);
        pb.redirectErrorStream(true);

        // 4) PATH 보정(IDE/서비스 환경 PATH 빈약 이슈 대비)
        Map<String, String> env = pb.environment();
        env.put("PATH", env.getOrDefault("PATH", "")
                + ":/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin");

        StringBuilder all = new StringBuilder(16 * 1024);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                all.append(line).append('\n');
                if (line.contains("[ERROR]")) {
                    System.err.println("▶ [Build][ERROR] " + line);
                } else {
                    System.out.println("▶ [Build] " + line);
                }
            }
        }

        // 5) 타임아웃(예: 20분)
        boolean finished = process.waitFor(20, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("❌ 빌드 타임아웃(20m)\n===== LOG (tail) =====\n" + tailLines(all.toString(), 200));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            // 흔한 케이스: 하위에 진짜 루트가 따로 있을 때 maven이 'no POM'을 토함 → 이미 root 재탐색으로 예방됨
            throw new RuntimeException("❌ 빌드 실패! exitCode=" + exitCode + "\n===== LOG (tail) =====\n" + tailLines(all.toString(), 200));
        }

        // 빌드 후 클래스 디렉토리 확인
        File mavenClasses = new File(root, "target/classes");
        File gradleClasses = new File(root, "build/classes/java/main");

        if (mavenClasses.exists()) {
            System.out.println("✅ 빌드 성공. Maven target/classes 생성됨 → " + mavenClasses.getAbsolutePath());
        } else if (gradleClasses.exists()) {
            System.out.println("✅ 빌드 성공. Gradle build/classes 생성됨 → " + gradleClasses.getAbsolutePath());
        } else {
            System.out.println("⚠️ 빌드는 성공했지만 target/classes 또는 build/classes 를 찾을 수 없음.");
        }
    }

    private void chmodX(File f) {
        try {
            if (f != null && f.exists()) {
                // macOS/Linux
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(f.toPath(), perms);
            }
        } catch (UnsupportedOperationException ignore) {
            // Windows면 무시
            if (f != null) f.setExecutable(true, true);
        } catch (Exception ignore) {
        }
    }

    private String tailLines(String text, int lines) {
        String[] arr = text.split("\n");
        int from = Math.max(0, arr.length - lines);
        return String.join("\n", Arrays.copyOfRange(arr, from, arr.length));
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
                "-Dsonar.organization=" + sonarOrg,
                "-Dsonar.token=" + sonarToken
//                "-Dsonar.login=" + sonarToken
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
                                 String projectKey) throws IOException, InterruptedException {
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

            // ✅ totalScore 계산 및 update
            analysis.setId(analyzeId);
            analysisService.updateTotalScore(analysis);
            System.out.println("✅ totalScore 업데이트 완료 - score: " + analysis.getTotalScore());

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
        try {
            return value == null ? null : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    public void deleteProject(String projectKey) {
        try {
//            String sonarBaseUrl = sonarHost;
            String deleteUrl = sonarHost + "/api/projects/delete?project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);

            String adminUsername = "admin";
            String adminPassword = "teamprojectY1!";

            HttpURLConnection connection = (HttpURLConnection) new URL(deleteUrl).openConnection();
            connection.setRequestMethod("POST");

            String basicAuth = "Basic " + Base64.getEncoder()
                    .encodeToString((adminUsername + ":" + adminPassword).getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Authorization", basicAuth);
//            String basicAuth = "Basic " + Base64.getEncoder()
//                    .encodeToString((sonarToken + ":").getBytes(StandardCharsets.UTF_8));
//            conn.setRequestProperty("Authorization", basicAuth);

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
                    "**/node_modules/**", "**/build/**", "**/dist/**", "**/target/**",
                    "**/.venv/**", "**/venv/**", "**/.tox/**", "**/.pytest_cache/**",
                    "**/.next/**", "**/.nuxt/**", "**/.yarn/**", "**/.pnpm-store/**"));

            // 3. 포함 패턴 (원본 코드 확장자)
            writer.println("sonar.inclusions=" + String.join(",",
                    "**/*.java", "**/*.kt", "**/*.kts", "**/*.py", "**/*.js", "**/*.jsx", "**/*.ts", "**/*.tsx"));

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

//            writer.println("sonar.login=" + sonarToken);
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
                        "target/classes", "target/test-classes",
                        "build/classes/java/main", "build/classes/java/test",
                        "build/classes/kotlin/main", "build/classes/kotlin/test"
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
        // 📦 GitHub zipball wrapper 처리 (repoName-commitHash)
        File[] children = baseDir.listFiles(File::isDirectory);
        if (children != null && children.length == 1 && children[0].getName().matches(".+-[0-9a-f]{5,}")) {
            System.out.println("⚠️ GitHub zipball wrapper 감지 → baseDir 교체: " + children[0].getAbsolutePath());
            baseDir = children[0];
        }

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