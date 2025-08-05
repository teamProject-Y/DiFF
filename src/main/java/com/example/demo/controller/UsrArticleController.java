package com.example.demo.controller;

import com.example.demo.vo.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.ArticleService;
import com.example.demo.service.CommentService;
import com.example.demo.service.ReactionService;
import com.example.demo.vo.Rq;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/DiFF/article")
public class UsrArticleController {

    private final BeforeActionInterceptor beforeActionInterceptor;

    @Autowired
    private Rq rq;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private CommentService commentService;

    UsrArticleController(BeforeActionInterceptor beforeActionInterceptor) {
        this.beforeActionInterceptor = beforeActionInterceptor;
    }

    @PostMapping("/list")
    public Map<String, Object> showList(@RequestBody Map<String, Object> param) {

        System.out.println("enter list");
        int page = (int) param.getOrDefault("page", 1);
        String keyword = (String) param.getOrDefault("keyword", "");
        int searchItem = (int) param.getOrDefault("searchItem", 0);

        System.out.println("page: " + page);
        System.out.println("keyword: " + keyword);
        System.out.println("searchItem: " + searchItem);

        int itemsInAPage = 10;
        int limitFrom = (page - 1) * itemsInAPage;

//        int totalCnt = articleService.getArticlesCnt(keyword, searchItem);
//        int totalPage = (int) Math.ceil(totalCnt / (double) itemsInAPage);
//        List<Article> articles = articleService.getArticles(keyword, searchItem, limitFrom, itemsInAPage);

        // 실제 로직 실행 후 응답
        Map<String, Object> result = new HashMap<>();
        result.put("result", "OK");
        result.put("page", page);
        return result;
    }
}