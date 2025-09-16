package com.example.demo.controller;

import com.example.demo.service.AnalysisService;
import com.example.demo.service.ArticleService;
import com.example.demo.service.RepositoryService;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/repository")
@RequiredArgsConstructor
public class UsrRepositoryController {

    @Autowired
    private  RepositoryService repositoryService;

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private ArticleService articleService;

    @GetMapping("/my")
    public ResultData<Map<String, Object>> getMyRepositories(HttpServletRequest req) {

        System.out.println("===== 📁👤 [Get] /api/DiFF/repository/my =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        List<Repository> repos = repositoryService.getRepositoriesByMemberId(memberId);
        System.out.println("📁👤 repo count = " + repos.size());
        for (Repository r : repos) {
            System.out.println("📁👤 repoId=" + r.getId() + ", name=" + r.getName() + "]");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("repositories", repos);

        return ResultData.from("S-1", "내 리포지토리 목록", data);
    }

    @GetMapping("/{repoId}/languages")
    public ResultData<List<Map<String, Object>>> getLanguageDistribution(@PathVariable Long repoId) {

        System.out.println("===== 📁🔤 [Get] /api/DiFF/repository/{repoId}/languages =====");

        List<Map<String, Object>> langs = repositoryService.getLanguageDistributionByRepo(repoId);
        System.out.println(" 📁🔤 repoId: " + repoId + " , 언어분포: " + langs);

        return ResultData.from("S-1", "언어 분포 조회 성공", langs);
    }

    @GetMapping("/{repoId}/history")
    public ResultData<List<Analysis>> getAnalysisHistory(@PathVariable Long repoId) {

        System.out.println("===== 📁📈 [Get] /api/DiFF/repository/{repoId}/history =====");

        List<Analysis> history = analysisService.getAnalysisHistory(repoId);
        System.out.println("📁📈  repoId: " + repoId + ", 분석이력: " + history);

        return ResultData.from("S-1", "분석 이력 조회 성공", history);
    }

    @GetMapping("/{repoId}/recent")
    public ResultData<List<Analysis>> getAnalysisRecent(@PathVariable Long repoId) {

        System.out.println("===== 📁📊 [Get] /api/DiFF/repository/{repoId}/recent =====");

        List<Analysis> recent = analysisService.getAnalysisRecent(repoId);
        System.out.println("📁📊 repoId"+repoId+"최근 2주 분석이력" + recent);

        return ResultData.from("S-1", "분석 이력 조회 성공", recent);
    }

    @PostMapping("/rename")
    public ResultData<Repository> renameRepository(HttpServletRequest req, @RequestBody Repository repo) {

        System.out.println("===== [POST] 📁✏️ /api/DiFF/repository/rename/=====");
        System.out.println("📁✏️ rename repoid: " + repo.getId() + ", new name: " + repo.getName());

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        // DB에서 실제 리포 가져오기
        Repository dbRepo = repositoryService.getRepositoryById(repo.getId());
        if (dbRepo == null) {
            return ResultData.from("F-404", "리포지토리가 존재하지 않습니다.");
        }

        // 로그 찍기
        System.out.printf("📁✏️ 로그인 ID = %d, 리포 소유자 ID = %d%n", loginedMemberId, dbRepo.getMemberId());

        if (!dbRepo.getMemberId().equals(loginedMemberId)) {
            System.out.println("📁✏️ 권한 없음 → 로그인 유저가 리포 주인이 아님");
            return ResultData.from("F-403", "권한 없음");
        }

        // 중복 이름 체크
        if (repositoryService.existsByMemberIdAndRepoName(loginedMemberId, repo.getName())) {
            System.out.println("📁✏️ 이미 존재하는 이름");
            return ResultData.from("F-500", "이미 존재하는 이름입니다.");
        }

        int affectedRow = repositoryService.renameRepository(repo.getId(), repo.getName());

        if (affectedRow == 0) {
            System.out.println("📁✏️ 업데이트 실패");
            return ResultData.from("F-1", "업데이트 실패");
        }

        return ResultData.from("S-1", "리포 이름 변경 성공");
    }

    @PostMapping("/connect")
    public ResultData<Repository> connectRepository(HttpServletRequest req,
                                                    Long repoId,
                                                    String url,
                                                    String owner,
                                                    String name,
                                                    String defaultBranch) {

        System.out.println("===== [POST] 📁🔗 /api/DiFF/repository/connect =====");
        System.out.println("📁🔗 connect url: " + url);

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        System.out.println("📁🔗️ repoId: "+ repoId);
        Repository dbRepo = repositoryService.getRepositoryById(repoId);
        System.out.println("📁🔗 DB Repo: " + dbRepo);
        if (dbRepo == null) {
            return ResultData.from("F-404", "리포지토리가 존재하지 않습니다.");
        }

        // 로그
        System.out.printf("📁🔗 로그인 ID = %d, 리포 소유자 ID = %d%n", loginedMemberId, dbRepo.getMemberId());

        if (!dbRepo.getMemberId().equals(loginedMemberId)) {
            System.out.println("📁🔗 권한 없음 → 로그인 유저가 리포 주인이 아님");
            return ResultData.from("F-403", "권한 없음");
        }

        int affectedRow = repositoryService.connectRepository(repoId, url, owner, name, defaultBranch);

        if (affectedRow == 0) {
            System.out.println("📁🔗 업데이트 실패");
            return ResultData.from("F-1", "업데이트 실패");
        }

        return ResultData.from("S-1", "리포 연결 성공");
    }

    @PostMapping("/createRepository")
    @ResponseBody
    public ResultData<Integer> createRepository(HttpServletRequest req , @RequestBody Repository repo) {

        System.out.println("===== [POST] 📁🆕 /api/DiFF/repository/createRepository =====");

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("📁🆕 insert repo " + repo);

        // 필수 값 검증
        if (repo.getName() == null || repo.getName().trim().isEmpty()) {
            return ResultData.from("F-2", "리포지토리 이름이 필요합니다.");
        }

        // 중복 확인
        if (repositoryService.existsByMemberIdAndRepoName(memberId, repo.getName())) {
            return ResultData.from("F-1", "이미 존재하는 리포지토리명입니다.");
        }

        repositoryService.insertRepository(
                memberId,
                repo.getName(),
                repo.isAPrivate(),
                repo.getUrl(),
                repo.getDefaultBranch(),
                repo.getOwner(),
                repo.getGithubName(),
                repo.getGithubOwner()
        );

        int newRepoId = repositoryService.getLastInsertId();

        return ResultData.from("S-1", "리포지토리가 생성되었습니다.", newRepoId);
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResultData<Integer> deleteRepository(HttpServletRequest req, @PathVariable Long id) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("===== 📁🗑️ [DELETE] /api/DiFF/repository/" + id + " =====");

        // 리포지토리 존재 여부 확인
        Repository repo = repositoryService.getRepositoryById(id);
        if (repo == null) {
            return ResultData.from("F-404", "해당 리포지토리가 존재하지 않습니다.");
        }

        // 권한 확인
        if (!repo.getMemberId().equals(loginedMemberId)) {
            return ResultData.from("F-403", "해당 리포지토리에 대한 권한이 없습니다.");
        }

        // 삭제
        int rows = repositoryService.deleteRepository(id, loginedMemberId);
        if (rows == 0) {
            return ResultData.from("F-500", "리포지토리 삭제 실패");
        }

        return ResultData.from("S-1", "The repository has been deleted", rows);
    }

    @GetMapping("/average/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getAverageMetrics(@PathVariable Long repositoryId) {

        System.out.println("===== 📁🧾 [GET] /api/DiFF/repository/average/{repositoryId} =====");

        return ResponseEntity.ok(analysisService.getAverageMetrics(repositoryId));
    }

    @GetMapping("/articles")
    public ResultData<List<Article>> getRepositoryArticles(HttpServletRequest req, Long repositoryId) {

        System.out.println("===== 📁📑 [GET] /api/DiFF/repository/articles =====");

        List<Article> articles = articleService.getRepositoryArticles(repositoryId);

        if(articles.size() == 0) {
            return ResultData.from("F-1", "게시물이 존재하지 않습니다.");
        }
        return ResultData.from("S-1", repositoryId + "번 리포 게시물 로딩 성공",  articles);
    }
}
