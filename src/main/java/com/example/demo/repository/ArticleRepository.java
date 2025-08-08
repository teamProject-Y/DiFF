package com.example.demo.repository;

import com.example.demo.vo.Article;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ArticleRepository {

    int getLastInsertId();

    int getArticlesCnt(Long repositoryId,String keyword, int searchItem);

    List<Article> getArticles(Long repositoryId, String keyword, int searchItem, int limitFrom, int itemsInAPage);

    int getArticleCnt();
}