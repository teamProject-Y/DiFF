package com.example.demo.repository;

import com.example.demo.vo.Article;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArticleRepository {

    int getLastInsertId();

     List<Article> getArticles(Long repositoryId, String keyword, int searchItem, Long loginedMemberId);

    List<Article> getTrendingArticles(Integer count, Integer days, Long loginedMemberId);

    int writeArticle(Article article);

    Article getArticleById(Long id);

    int modifyArticle(Article article);

    int deleteArticle(Long id, Long memberId);

    List<Article> getFollowingArticles(int limitFrom, int itemsInAPage, Long loginedMemberId);

    int getFollowingArticlesCnt(Long memberId, Long repositoryId, String keyword, int searchItem);

    int increaseHits(Long articleId);

    List<Article> getRepositoryArticles(Long repositoryId);

    List<Article> searchArticles(String keyword, Long loginedMemberId);

    Long getArticleCountsByMemberId(Long id);

    Long getWriterIdByArticleId(Long articleId);
}