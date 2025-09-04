package com.example.demo.repository;

import com.example.demo.vo.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ArticleRepository {

    int getLastInsertId();

    int getArticlesCnt(Long repositoryId,String keyword, int searchItem);

     List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage);

    int getArticleCnt();

    List<Article> getTrendingArticles(Integer count, Integer days);

    int writeArticle(Article article);

    Article getArticleById(Long id);

    int modifyArticle(Article article);

    int deleteArticle(Long id, Long memberId);

    List<Article> getFollowingArticles(Long memberId, int limitFrom, int itemsInAPage);

    int getFollowingArticlesCnt(Long memberId, Long repositoryId, String keyword, int searchItem);

    int increaseHits(Long articleId);

    List<Article> getRepositoryArticles(Long repositoryId);

    List<Article> searchArticles(String keyword);

    Long getArticleCountsByMemberId(Long id);

    Long getWriterIdByArticleId(Long articleId);
}