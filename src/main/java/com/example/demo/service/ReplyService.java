package com.example.demo.service;

import com.example.demo.repository.ReplyRepository;
import com.example.demo.vo.Reply;
import com.example.demo.vo.ResultData;
import com.example.util.Ut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ReplyRepository;
import com.example.demo.repository.ReactionRepository;

import java.util.List;

@Service
public class ReplyService {

    @Autowired
    private ReplyRepository replyRepository;

    @Autowired
    private ReactionRepository reactionRepository;

    public ReplyService(ReplyRepository replyRepository) {
        this.replyRepository = replyRepository;
    }


    public int doReplyWrtie(Long articleId, Long loginedMemberId, String body) {
        return replyRepository.doReplyWrtie(articleId, loginedMemberId, body);
    }

    public List<Reply> getReplys(Long articleId, Long loginedMemberId) {

        List<Reply> replys = replyRepository.getReplys(articleId);
        updateForPrintData(loginedMemberId, replys);

        return replys;
    }

    private void updateForPrintData(Long loginedMemberId, List<Reply> replys) {

        if (replys.size() == 0)
            return;

        for (Reply reply : replys) {

            ResultData userCanModifyRd = userCanModify(loginedMemberId, reply);
            reply.setUserCanModify(userCanModifyRd.isSuccess());

            ResultData userCanDeleteRd = userCanDelete(loginedMemberId, reply);
            reply.setUserCanDelete(userCanModifyRd.isSuccess());

            ResultData userReactionRd = userReaction(loginedMemberId, reply.getId());
            if (userReactionRd == null) continue;

            reply.setUserReaction((boolean) userReactionRd.getData1());
        }

    }

    private ResultData userReaction(Long loginedMemberId, Long id) {

        Long isReactioned = reactionRepository.getIsReactioned(loginedMemberId, id, "reply");
        if (isReactioned == 0)
            return ResultData.from("F-1", Ut.f("%d번 댓글 반응", id), "없음", isReactioned);
        return ResultData.from("S-1", Ut.f("%d번 댓글 반응", id), "있음", isReactioned);
    }

    private ResultData userCanModify(Long loginedMemberId, Reply reply) {

        if (reply.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 수정 권한 없음", reply.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 수정 권한 있음", reply.getId()));
    }

    private ResultData userCanDelete(Long loginedMemberId, Reply reply) {

        if (reply.getMemberId() != loginedMemberId) {
            return ResultData.from("F-A", Ut.f("%d번 게시글 삭제 권한 없음", reply.getId()));
        }

        return ResultData.from("S-1", Ut.f("%d번 게시글 삭제 권한 있음", reply.getId()));
    }
}