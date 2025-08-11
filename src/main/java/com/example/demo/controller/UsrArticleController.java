package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.ArticleService;
import com.example.demo.service.CommentService;
import com.example.demo.service.ReactionService;
import com.example.demo.vo.Article;
import com.example.demo.vo.Rq;

@RestController
@RequestMapping("/api/DiFF/article")
public class UsrArticleController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ArticleService articleService;

    UsrArticleController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> showList(
            @RequestParam(defaultValue = "repositoryId") Long repositoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int searchItem) {
        System.out.println("📥 /api/DiFF/article/list 요청 도착 repoId: "+repositoryId);
        System.out.println("➡️ page: " + page);
        System.out.println("➡️ searchItem: " + searchItem);
        System.out.println("➡️ keyword: " + keyword);
        int itemsInAPage = 10;
        int limitFrom = (page - 1) * itemsInAPage;

        int totalCnt = articleService.getArticlesCnt(repositoryId, keyword, searchItem);
        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage);
        List<Article> articles = articleService.getArticles(repositoryId, keyword, searchItem, limitFrom, itemsInAPage);

        System.out.println("📤 조회된 게시글 수: " + articles.size());

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);
        result.put("totalCnt", totalCnt);
        result.put("totalPage", totalPage);
        result.put("page", page);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/trending")
    public ResponseEntity<Map<String, Object>> getTrending(
            @RequestParam(defaultValue = "100") Integer count,
            @RequestParam(defaultValue = "30") Integer days) {
        System.out.println("📥 /api/DiFF/article/trending 요청 도착");

        System.out.println("-> count: " + count);
        System.out.println("-> days: " + days);

        List<Article> articles = articleService.getTrendingArticles(count, days);

        Map<String, Object> result = new HashMap<>();
        result.put("articles", articles);

        return ResponseEntity.ok(result);
    }
}

