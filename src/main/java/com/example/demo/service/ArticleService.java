package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ArticleRepository;
import com.example.demo.repository.ReactionRepository;
import com.example.demo.vo.Article;
import com.example.demo.vo.ResultData;

import util.Ut;

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

    public Article writeArticle(String title, String body, int memberId, int boardId) {
        articleRepository.writeArticle(title, body, memberId, boardId);
        return new Article(title, body, memberId, boardId);
    }

    public void deleteArticle(int id) {
        articleRepository.deleteArticle(id);
    }

    public void modifyArticle(int id, String title, String body) {
        articleRepository.modifyArticle(id, title, body);
    }

    public Article getArticleById(int id) {
        return articleRepository.getArticleById(id);
    }

    public List<Article> getArticles(String keyword, int boardId, int searchItem, int limitFrom, int itemsInAPage) {
        return articleRepository.getArticles(keyword, boardId, searchItem, limitFrom, itemsInAPage);
    }

    public Article getArticleForPrint(int id, int loginedMemberId) {

        Article article = articleRepository.getArticleForPrint(id);

        updateForPrintData(loginedMemberId, article);

        return article;
    }


    private void updateForPrintData(int loginedMemberId, Article article) {
        if (article == null) return;

        ResultData userCanModifyRd = userCanModify(loginedMemberId, article);
        article.setUserCanModify(userCanModifyRd.isSuccess());

        ResultData userCanDeleteRd = userCanDelete(loginedMemberId, article);
        article.setUserCanDelete(userCanModifyRd.isSuccess());

        ResultData userReactionRd = userReaction(loginedMemberId, article.getId());
        if(userReactionRd == null) return;
        article.setUserReaction((int)userReactionRd.getData1());

    }


    public ResultData userReaction(int loginedMemberId, int id) { //

        int isReactioned = reactionRepository.getIsReactioned(loginedMemberId, id, "article");
        if(isReactioned == 0) return null;

        int reactionPoint = reactionRepository.getUserReaction(loginedMemberId, id, "article");

        if(reactionPoint == 0) return ResultData.from("F-1",Ut.f("%d번 게시글 반응", id), "없음", reactionPoint);
        else if(reactionPoint == 1) return ResultData.from("S-1",Ut.f("%d번 게시글 반응", id), "좋아요", reactionPoint);
        else return ResultData.from("S-2",Ut.f("%d번 게시글 반응", id), "싫어요", reactionPoint);

    }


    public ResultData userCanModify(int loginedMemberId, Article article) {

        if (article.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 수정 권한 없음", article.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 수정 권한 있음", article.getId()));
    }

    private ResultData userCanDelete(int loginedMemberId, Article article) {

        if (article.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 삭제 권한 없음", article.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 삭제 권한 있음", article.getId()));
    }


    public int getArticleCnt() {

        return articleRepository.getArticleCnt();
    }


    public int getArticlesCnt(String keyword, int boardId, int searchItem) {
        return articleRepository.getArticlesCnt(keyword, boardId, searchItem);
    }


    public ResultData doIncHits(int id) {

        int affectedRow = articleRepository.doIncHits(id);

        if (affectedRow == 0) {
            return ResultData.from("F-1", "해당 게시글 없음", "id", id);
        }

        return ResultData.from("S-1", "조회수 증가", "id", id);
    }


    public int getLikes(int id) {
        return articleRepository.getLikes(id);
    }


    public int getHits(int id) {
        return articleRepository.getHits(id);
    }

}