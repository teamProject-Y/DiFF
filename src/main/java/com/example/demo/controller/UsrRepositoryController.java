package com.example.demo.controller;

import com.example.demo.service.AnalysisService;
import com.example.demo.service.ArticleService;
import com.example.demo.service.RepositoryService;
import com.example.demo.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsPasswordService;
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
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();

        System.out.println("\n===== [GET] /api/DiFF/repository/my =====");
        System.out.println("memberId = " + memberId);

        List<Repository> repos = repositoryService.getRepositoriesByMemberId(memberId);
        System.out.println("repo count = " + repos.size());
        for (Repository r : repos) {
            System.out.println(" - repoId=" + r.getId() + ", name=" + r.getName() + "]");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("repositories", repos);

        return ResultData.from("S-1", "내 리포지토리 목록", data);
    }

    @GetMapping("/{repoId}/languages")
    public ResultData<List<Map<String, Object>>> getLanguageDistribution(@PathVariable Long repoId) {
        List<Map<String, Object>> langs = repositoryService.getLanguageDistributionByRepo(repoId);
        System.out.println("repoId"+repoId+"언어분포" + langs);
        return ResultData.from("S-1", "언어 분포 조회 성공", langs);
    }

    @GetMapping("/{repoId}/history")
    public ResultData<List<Analysis>> getAnalysisHistory(@PathVariable Long repoId) {
        List<Analysis> history = analysisService.getAnalysisHistory(repoId);
        System.out.println("repoId"+repoId+"분석이력" + history);
        return ResultData.from("S-1", "분석 이력 조회 성공", history);
    }

    @PostMapping("/rename")
    public ResultData<Repository> renameRepository(HttpServletRequest req, @RequestBody Repository repo) {

        System.out.println("\n===== [POST] /api/DiFF/repository/rename/=====");
        System.out.println("rename repoid: " +  repo.getId() + ", name: " + repo.getName());

        Rq rq = (Rq) req.getAttribute("rq");
        Long loginedMemberId = rq.getLoginedMemberId();

        if (loginedMemberId != repo.getMemberId()) {
            System.out.println("loginedMemberId is not equal to repo.getMemberId()");
            return ResultData.from("F-400", "권한 없음");
        }

        if(repositoryService.existsByMemberIdAndRepoName(loginedMemberId, repo.getName())) {
            System.out.println("이미 존재하는 이름");
            return ResultData.from("F-500", "이미 존재하는 이름입니다.");
        }

        int affectedRow = repositoryService.renameRepository(repo.getId(), repo.getName());

        if(affectedRow == 0) {
            System.out.println("왠지 실패");
            return ResultData.from("F-1", "왠지 실패");
        }

        return ResultData.from("S-1", "success");
    }

    @PostMapping("/createRepository")
    @ResponseBody
    public ResultData<Integer> createRepository(HttpServletRequest req , @RequestBody Repository repo) {
        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = ((Number) rq.getLoginedMemberId()).longValue();
        System.out.println("\n===== [POST] /api/DiFF/repository/createRepository =====");
        System.out.println("repo is private? " + repo.isAPrivate());
        System.out.println("description: ");
        System.out.println("repository name: " + repo.getName());

        System.out.println("🐳 insert repo" + repo);

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
                repo.getOwner()
        );

        int newRepoId = repositoryService.getLastInsertId();

        return ResultData.from("S-1", "리포지토리가 생성되었습니다.", newRepoId);
    }

    @GetMapping("/average/{repositoryId}")
    public ResponseEntity<Map<String, Object>> getAverageMetrics(@PathVariable Long repositoryId) {
        return ResponseEntity.ok(analysisService.getAverageMetrics(repositoryId));
    }

    @GetMapping("/articles")
    public ResultData<List<Article>> getRepositoryArticles(HttpServletRequest req, Long repositoryId) {

        System.out.println("🪱 repository controller - gerRepositoryArticles");
        System.out.println("RepositoryId: " + repositoryId);

        Rq rq = (Rq) req.getAttribute("rq");
        Long memberId = rq.getLoginedMemberId();

        boolean IsRepoOwner = repositoryService.isRepoOwner(memberId, repositoryId);

        List<Article> articles = articleService.getRepositoryArticles(repositoryId);

        if(articles.size() == 0) {
            return ResultData.from("F-1", "게시물이 존재하지 않습니다.");
        }

        return ResultData.from("S-1", repositoryId + "번 리포 게시물 로딩 성공",  articles);
    }
}
