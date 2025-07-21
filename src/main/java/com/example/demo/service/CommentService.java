package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.ReactionRepository;
import com.example.demo.vo.Comment;
import com.example.demo.vo.ResultData;

import util.Ut;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    public List<Comment> getComments(int relId, int loginedMemberId) {

        List<Comment> comments = commentRepository.getComments(relId);
        updateForPrintData(loginedMemberId, comments);

        return comments;
    }

    private void updateForPrintData(int loginedMemberId, List<Comment> comments) {

        if (comments.size() == 0)
            return;

        for (Comment comment : comments) {

            ResultData userCanModifyRd = userCanModify(loginedMemberId, comment);
            comment.setUserCanModify(userCanModifyRd.isSuccess());

            ResultData userCanDeleteRd = userCanDelete(loginedMemberId, comment);
            comment.setUserCanDelete(userCanModifyRd.isSuccess());

            ResultData userReactionRd = userReaction(loginedMemberId, comment.getId());
            if (userReactionRd == null) continue;

            comment.setUserReaction((int) userReactionRd.getData1());
        }

    }

    private ResultData userCanModify(int loginedMemberId, Comment comment) {

        if (comment.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 수정 권한 없음", comment.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 수정 권한 있음", comment.getId()));
    }

    private ResultData userCanDelete(int loginedMemberId, Comment comment) {

        if (comment.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 삭제 권한 없음", comment.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 삭제 권한 있음", comment.getId()));
    }

    public ResultData userReaction(int loginedMemberId, int id) { //

        int isReactioned = reactionRepository.getIsReactioned(loginedMemberId, id, "comment");
        if (isReactioned == 0) return null;

        int reactionPoint = reactionRepository.getUserReaction(loginedMemberId, id, "comment");

        if (reactionPoint == 0)
            return ResultData.from("F-1", Ut.f("%d번 게시글 반응", id), "없음", reactionPoint);
        else if (reactionPoint == 1)
            return ResultData.from("S-1", Ut.f("%d번 게시글 반응", id), "좋아요", reactionPoint);
        else
            return ResultData.from("S-2", Ut.f("%d번 게시글 반응", id), "싫어요", reactionPoint);

    }

    public int doCommentWrtie(String relTypeCode, int relId, int loginedMemberId, String body) {
        return commentRepository.doCommentWrtie(relTypeCode, relId, loginedMemberId, body);
    }

}