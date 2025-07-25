package com.example.demo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.interceptor.BeforeActionInterceptor;
import com.example.demo.service.ArticleService;
import com.example.demo.service.CommentService;
import com.example.demo.service.ReactionService;
import com.example.demo.vo.Article;
import com.example.demo.vo.Comment;
import com.example.demo.vo.ResultData;
import com.example.demo.vo.Rq;

import jakarta.servlet.http.HttpServletRequest;
import util.Ut;

@Controller
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

}