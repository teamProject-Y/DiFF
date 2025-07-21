package com.example.demo.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

import com.example.demo.vo.Article;

@Mapper
public interface ArticleRepository {

    //	@Insert("INSERT INTO article SET regDate = NOW(), updateDate = NOW(), title = #{title}, `body` = #{body}")
    public int writeArticle(String title, String body, int memberId, int boardId);

    //	@Delete("DELETE FROM article WHERE id = #{id}")
    public void deleteArticle(int id);

    //	@Update("UPDATE article SET updateDate = NOW(), title = #{title}, `body` = #{body} WHERE id = #{id}")
    public void modifyArticle(int id, String title, String body);

    //	@Select("SELECT * FROM article WHERE id = #{id}")
    public Article getArticleById(int id);

    //	@Select("SELECT A.*, M.name as extra_writerName FROM article A JOIN `member` M ON A.writerId = M.id WHERE A.id = #{id}")
    public Article getArticleForPrint(int id);

    //	@Select("SELECT A.*, M.name as extra_writerName FROM article A JOIN `member` M ON A.writerId = M.id ORDER BY A.id DESC")
    public List<Article> getArticles(String keyword, int boardId, int searchItem, int limitFrom, int itemsInAPage);

    //  @Select("SELECT LAST_INSERT_ID();")
    public int getLastInsertId();

    //  @Select("SELECT COUNT(*) FROM article")
    public int getArticleCnt();

    public int getArticlesCnt(String keyword, int boardId, int searchItem);

    public int doIncHits(int id);

    public int getLikes(int id);

    public int isMyreaction(int articleId, int loginedMemberId);

    public int getHits(int id);
}