package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import com.example.demo.repository.ReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ArticleRepository;
import com.example.demo.vo.Article;
import com.example.demo.vo.ResultData;

import com.example.util.Ut;

@Service
public class ArticleService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }


    public int getLastInsertId() {
        return articleRepository.getLastInsertId();
    }


    public int getArticlesCnt(Long repository, String keyword, int searchItem) {
        return articleRepository.getArticlesCnt(repository, keyword, searchItem);
    }

    public List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage) {
        return articleRepository.getArticles(repositoryId, keyword, searchItem, limitFrom, itemsInAPage);
    }

    public List<Article> getTrendingArticles(Integer count, Integer days) {
        return articleRepository.getTrendingArticles(count, days);
    }

    public int writeArticle(Long memberId, String title, String body, String checksum, Long repositoryId) {
        return articleRepository.writeArticle(memberId, title, body, checksum, repositoryId);
    }

    public Article getArticleById(Long id) {
        return articleRepository.getArticleById(id);
    }

    public int modifyArticle(Article article) {
        return articleRepository.modifyArticle(article); // update된 row 수 반환
    }

    public int deleteArticle(Long id, Long memberId) {
        return articleRepository.deleteArticle(id, memberId);
    }
}