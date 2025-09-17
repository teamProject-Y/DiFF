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
import org.springframework.http.*;
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

    @Autowired private AnalysisRepository analysisRepository;
    @Autowired private DraftRepository draftRepository;
    @Autowired private AnalysisService analysisService;

    @Value("${sonar.host}") private String sonarHost;             // https://sonarcloud.io
    @Value("${sonar.token}") private String sonarToken;           // SonarCloud token
    @Value("${sonar.organization}") private String sonarOrg;      // e.g. yullc

    /** ZIP 업로드 → 해제 → 빌드(테스트+JaCoCo) → sonar-project.properties 생성 */
//    public String extractAndPrepare(MultipartFile zipFile, String projectKey) throws IOException, InterruptedException {
//        Path tempDir = Files.createTempDirectory("source-");
//        File targetDir = tempDir.toFile();
//
//        // 1) ZIP 저장 및 해제
//        File tempZip = File.createTempFile("upload-", ".zip");
//        zipFile.transferTo(tempZip);
//        unzip(tempZip, targetDir);
//
//        // 2) GitHub zipball wrapper(repo-<sha>) 폴더 보정
//        File[] children = targetDir.listFiles(File::isDirectory);
//        if (children != null && children.length == 1) {
//            File wrapper = children[0];
//            if (wrapper.getName().matches(".+-[0-9a-f]{5,}.*")) {
//                System.out.println("⚠️ zipball wrapper 감지 → baseDir 교체: " + wrapper.getAbsolutePath());
//                targetDir = wrapper;
//            }
//        }
//
//        // 3) 빌드(테스트+JaCoCo 리포트 생성)
//        runBuild(targetDir);
//
//        // 4) 정적 설정 파일 생성(민감/동적 값은 파일에 쓰지 않음)
//        createSonarPropertiesFile(targetDir, projectKey);
//
//        return targetDir.getAbsolutePath();
//    }

    /** 빌드 루트(pom/gradle) 탐색 */
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
                File[] ch = cur.listFiles(File::isDirectory);
                if (ch != null) for (File c : ch) q.add(c);
            }
            depth++;
        }
        return null;
    }

    private void runBuild(File dir) throws IOException, InterruptedException {
        File root = findProjectRoot(dir, 3);
        if (root == null) {
            System.out.println("⚠️ Maven/Gradle 프로젝트 아님. 빌드 스킵");
            return;
        }

        File pom = new File(root, "pom.xml");
        File gradle = new File(root, "build.gradle");
        File gradleKts = new File(root, "build.gradle.kts");
        File mvnw = new File(root, "mvnw");
        File gradlew = new File(root, "gradlew");

        if (new File(root, "target/classes").exists()
                || new File(root, "build/classes/java/main").exists()) {
            System.out.println("🔎 Prebuilt artifacts detected → build step skip");
            return;
        }

        chmodX(mvnw);
        chmodX(gradlew);

        String cmd;
        if (pom.exists()) {
            String mvnCmd = mvnw.exists() ? "./mvnw" : "mvn";

            String repoArg = "";

            cmd = mvnCmd + repoArg + " -B -e clean verify";
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
        Map<String,String> env = pb.environment();
        env.put("PATH", env.getOrDefault("PATH","") + ":/usr/local/bin:/usr/bin:/bin");

        StringBuilder all = new StringBuilder(16 * 1024);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                all.append(line).append('\n');
                if (line.contains("[ERROR]")) System.err.println("▶ [Build][ERROR] " + line);
                else System.out.println("▶ [Build] " + line);
            }
        }

        boolean ok = p.waitFor(20, java.util.concurrent.TimeUnit.MINUTES);
        if (!ok) { p.destroyForcibly(); throw new RuntimeException("❌ 빌드 타임아웃"); }
        if (p.exitValue() != 0) throw new RuntimeException("❌ 빌드 실패\n" + all);
        System.out.println("✅ build success");
    }

    private void chmodX(File f) {
        try {
            if (f != null && f.exists()) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x");
                Files.setPosixFilePermissions(f.toPath(), perms);
            }
        } catch (UnsupportedOperationException ignore) {
            if (f != null) f.setExecutable(true, true);
        } catch (Exception ignore) {}
    }

//    private String tailLines(String text, int lines) {
//        String[] arr = text.split("\n");
//        int from = Math.max(0, arr.length - lines);
//        return String.join("\n", Arrays.copyOfRange(arr, from, arr.length));
//    }

    private void unzip(File zipFile, File destDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                File out = new File(destDir, e.getName());
                if (e.isDirectory()) out.mkdirs();
                else {
                    out.getParentFile().mkdirs();
                    try (InputStream is = zip.getInputStream(e);
                         FileOutputStream fos = new FileOutputStream(out)) {
                        is.transferTo(fos);
                    }
                }
            }
        }
    }

    /** Sonar 분석: Maven 플러그인으로 실행(별도 CLI 설치 불필요) */
    public void runSonarScanner(String dir, String projectKey) throws IOException, InterruptedException {
        File work = new File(dir);
        String mvn = new File(work, "mvnw").exists() ? "./mvnw" : "mvn";

        // 1) Maven 로컬 저장소를 작업 디렉터리 밑으로(쓰기권한 보장)
        File m2 = new File(work, ".m2");
        if (!m2.isDirectory() && !m2.mkdirs()) {
            throw new IOException("Cannot create local maven repo: " + m2.getAbsolutePath());
        }

        // 2) jacoco.xml들 모두 수집(멀티모듈 대응). 없으면 빈 문자열.
        String jacocoPaths = java.nio.file.Files.walk(work.toPath())
                .filter(p -> p.getFileName().toString().equals("jacoco.xml"))
                .map(p -> work.toPath().relativize(p).toString()) // 상대경로로 넘기면 깔끔
                .collect(java.util.stream.Collectors.joining(","));

        java.util.List<String> cmd = new java.util.ArrayList<>();

        cmd.add(mvn);
        cmd.add("-B");
        cmd.add("--no-transfer-progress");
//        cmd.add("-Dmaven.repo.local=" + m2.getAbsolutePath());
        cmd.add("-Dmaven.repo.local=/tmp/.m2");
        cmd.add("org.sonarsource.scanner.maven:sonar-maven-plugin:sonar");
        cmd.add("-Dsonar.host.url=" + sonarHost);
        cmd.add("-Dsonar.projectKey=" + projectKey);
        if (sonarOrg != null && !sonarOrg.isBlank()) {
            cmd.add("-Dsonar.organization=" + sonarOrg); // SonarCloud만 해당
        }
        if (!jacocoPaths.isBlank()) {
            cmd.add("-Dsonar.coverage.jacoco.xmlReportPaths=" + jacocoPaths);
        }

        String joined = String.join(" ", cmd);
        System.out.println("▶ Sonar cmd: " + joined);

        ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-lc", joined);
        pb.directory(work);
        pb.redirectErrorStream(true);

        // 3) 토큰은 환경변수로(플러그인이 SONAR_TOKEN 읽음)
        java.util.Map<String,String> env = pb.environment();
        env.put("SONAR_TOKEN", sonarToken);   // 최신 권장
        env.put("SONAR_LOGIN", sonarToken);   // 구버전 호환(있어도 문제 없음)

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line; while ((line = r.readLine()) != null) System.out.println("▶ [Sonar] " + line);
        }
        if (!p.waitFor(30, TimeUnit.MINUTES)) { p.destroyForcibly(); throw new RuntimeException("❌ Sonar timeout(30m)"); }
        if (p.exitValue() != 0) throw new RuntimeException("❌ Sonar 실패(exit=" + p.exitValue() + ")");
    }

    /** 분석 결과 수집(간단 폴링) */
    public String getAnalysisResult(String projectKey) throws InterruptedException {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(sonarToken, "");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 1) Compute Engine 상태 폴링 (component별 큐)
        String statusUrl = sonarHost + "/api/ce/component?component=" + projectKey;
        int maxRetries = 10, delayMs = 2000;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ResponseEntity<String> res = rest.exchange(statusUrl, HttpMethod.GET, entity, String.class);
                String body = res.getBody();
                if (body != null && body.contains("\"status\":\"SUCCESS\"")) {
                    System.out.println("✅ Sonar 분석 완료 감지");
                    break;
                }
                System.out.println("⌛ 분석 대기중... " + (i + 1) + "/" + maxRetries);
                Thread.sleep(delayMs);
            } catch (Exception e) {
                System.out.println("상태 확인 실패: " + e.getMessage());
                Thread.sleep(delayMs);
            }
        }

        // 2) Measures 조회
        String measuresUrl = sonarHost + "/api/measures/component?component=" + projectKey
                + "&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,complexity,ncloc_language_distribution";
        System.out.println("measuresUrl : " + measuresUrl);
        for (int i = 0; i < 10; i++) {
            try {
                ResponseEntity<String> res = rest.exchange(measuresUrl, HttpMethod.GET, entity, String.class);
                System.out.println("✅ 분석 결과 가져오기 성공");
                return res.getBody();
            } catch (HttpClientErrorException.NotFound e) {
                System.out.println("⏳ 분석 결과 대기중... " + (i + 1) + "/10");
                Thread.sleep(delayMs);
            }
        }
        throw new RuntimeException("분석 결과를 가져오지 못했습니다: " + projectKey);
    }

    /** DB 저장 */
    public void analysisInsertDB(Long repositoryId, Long memberId, Long draftId, Long diffId, String checksum, String projectKey)
            throws IOException, InterruptedException {
        try {
            String resultJson = getAnalysisResult(projectKey);
            if (resultJson == null || !resultJson.trim().startsWith("{")) {
                System.out.println("❌ 분석 결과 JSON 아님: " + resultJson);
                return;
            }

            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(resultJson);
            JsonNode component = root.get("component");
            String projectKeyFromJson = component.get("key").asText();
            String projectName = component.get("name").asText();
            JsonNode measures = component.get("measures");

            Map<String, String> metricMap = new HashMap<>();
            for (JsonNode m : measures) {
                metricMap.put(m.get("metric").asText(), m.get("value").asText());
            }

            Draft draft = draftRepository.getDraftById(draftId);
            if (draft == null) {
                System.out.println("❌ draftId=" + draftId + " 에 해당 Draft 없음!");
                return;
            }
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

            analysisRepository.insert(analysis);
            Long analyzeId = analysis.getId();
            System.out.println("✅ 분석 결과 저장 완료 - analyzeId: " + analyzeId);

            analysis.setId(analyzeId);
            analysisService.updateTotalScore(analysis);
            System.out.println("✅ totalScore 업데이트 완료 - score: " + analysis.getTotalScore());

            String langRaw = metricMap.get("ncloc_language_distribution");
            if (langRaw != null) {
                List<AnalysisLanguage> langs = parseLanguageDistribution(langRaw, analyzeId);
                for (AnalysisLanguage l : langs) analysisRepository.insertLanguage(l);
                System.out.println("✅ 언어 분포 저장 완료 - " + langs.size() + "개 언어");
            }

        } catch (Exception e) {
            System.out.println("❌ analysisInsertDB 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<AnalysisLanguage> parseLanguageDistribution(String raw, Long analyzeId) {
        List<AnalysisLanguage> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String pair : raw.split(";")) {
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

    /** SonarCloud 프로젝트 삭제(토큰 BasicAuth) */
    public void deleteProject(String projectKey) {
        try {
            String deleteUrl = sonarHost + "/api/projects/delete?project=" + URLEncoder.encode(projectKey, StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
            conn.setRequestMethod("POST");
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString((sonarToken + ":").getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", basicAuth);

            int code = conn.getResponseCode();
            if (code == 204) {
                System.out.println("✅ 프로젝트 삭제 성공: " + projectKey);
            } else {
                InputStream es = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(es, StandardCharsets.UTF_8))) {
                    System.out.println("❌ 프로젝트 삭제 실패(" + code + "): " + in.lines().collect(Collectors.joining()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 정적 설정만 파일에 반영(민감/동적 값은 CLI -D 로) */
    private void createSonarPropertiesFile(File projectDir, String projectKey) throws IOException {
        File propertiesFile = new File(projectDir, "sonar-project.properties");

        List<String> sourcePaths = detectAllValidSourceFolders(projectDir).stream()
                .map(path -> projectDir.toPath().relativize(Path.of(path)).toString())
                .distinct().collect(Collectors.toList());

        List<String> javaBins = findAllJavaBinaries(projectDir).stream()
                .map(path -> projectDir.toPath().relativize(Path.of(path)).toString())
                .distinct().collect(Collectors.toList());

        boolean containsJava = !javaBins.isEmpty();
        boolean containsPY   = containsExtension(projectDir, ".py");
        boolean containsJS   = containsExtension(projectDir, ".js");

        try (PrintWriter w = new PrintWriter(propertiesFile)) {
            w.println("sonar.projectKey=" + projectKey);
            w.println("sonar.projectName=" + projectKey);
            w.println("sonar.projectVersion=1.0");
            w.println("sonar.host.url=" + sonarHost); // CLI -D 가 우선하므로 있어도 무방

            w.println("sonar.sources=" + String.join(",", sourcePaths));
            w.println("sonar.exclusions=" + String.join(",",
                    "**/node_modules/**","**/build/**","**/dist/**","**/target/**",
                    "**/.venv/**","**/venv/**","**/.tox/**","**/.pytest_cache/**",
                    "**/.next/**","**/.nuxt/**","**/.yarn/**","**/.pnpm-store/**"));
            w.println("sonar.inclusions=" + String.join(",",
                    "**/*.java","**/*.kt","**/*.kts","**/*.py","**/*.js","**/*.jsx","**/*.ts","**/*.tsx"));

            if (containsJava) {
                w.println("sonar.java.binaries=" + String.join(",", javaBins));
                w.println("sonar.java.source=17");
            }
            if (containsPY) {
                w.println("sonar.python.version=3.10");
            }
            // ❌ 민감/동적 값 금지: sonar.token / sonar.organization / login 등은 쓰지 않음
        }

        System.out.println("📝 sonar-project.properties 생성 완료 @ " + propertiesFile.getAbsolutePath());
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
        File[] children = baseDir.listFiles(File::isDirectory);
        if (children != null && children.length == 1 && children[0].getName().matches(".+-[0-9a-f]{5,}")) {
            baseDir = children[0];
        }
        String[] candidates = {"src", "client", "apps", "js", "python", "."};
        List<String> valid = new ArrayList<>();
        for (String name : candidates) {
            File d = new File(baseDir, name);
            if (d.exists() && d.isDirectory()) valid.add(d.getAbsolutePath());
        }
        if (valid.isEmpty()) valid.add(baseDir.getAbsolutePath());
        return valid;
    }

    private boolean containsExtension(File dir, String ext) {
        if (!dir.exists() || !dir.isDirectory()) return false;
        File[] files = dir.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isDirectory()) { if (containsExtension(f, ext)) return true; }
            else if (f.getName().endsWith(ext)) return true;
        }
        return false;
    }

    public String extractAndPrepare(File zipFileOnDisk, String projectKey)
            throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("source-");
        File targetDir = tempDir.toFile();

        unzip(zipFileOnDisk, targetDir);          // 기존 unzip(File, File) 사용
        // zipball wrapper 보정, runBuild(targetDir), createSonarPropertiesFile(targetDir, projectKey)
        // 는 네가 가진 기존 로직 그대로 호출
        runBuild(targetDir);
        createSonarPropertiesFile(targetDir, projectKey);
        return targetDir.getAbsolutePath();
    }
}
