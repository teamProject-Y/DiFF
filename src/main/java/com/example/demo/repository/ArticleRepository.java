package com.example.demo.repository;

import com.example.demo.vo.Article;
import com.example.demo.vo.ResultData;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ArticleRepository {

    public int getLastInsertId();

    public int getArticlesCnt(Long repositoryId,String keyword, int searchItem);

    public  List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage);

    public int getArticleCnt();

    public List<Article> getTrendingArticles(Integer count, Integer days);

    public int writeArticle(Long memberId, String title, String body, String checksum, Long repositoryId, Long draftId);

    public Article getArticleById(Long id);

    public int modifyArticle(Article article);

    public int deleteArticle(Long id, Long memberId);

    public List<Article> getFollowingArticles(Long memberId, int limitFrom, int itemsInAPage);

    public int getFollowingArticlesCnt(Long memberId, Long repositoryId, String keyword, int searchItem);
}