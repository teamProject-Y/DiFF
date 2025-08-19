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
import java.util.Optional;

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

    public List<Reply> getReplies(Long articleId, Long loginedMemberId) {

        List<Reply> replys = replyRepository.getReplies(articleId);
        System.err.println("replyservice에서 reply 개수: " + replys.size());
        updateForPrintData(loginedMemberId, replys);

        return replys;
    }

    public void updateForPrintData(Long loginedMemberId, List<Reply> replies) {
        if (replies == null || replies.isEmpty()) return;

        for (Reply reply : replies) {
            reply.setUserCanModify(canModify(loginedMemberId, reply));
            reply.setUserCanDelete(canDelete(loginedMemberId, reply));
//            reply.setUserReaction(hasReaction(loginedMemberId, reply.getId()));
        }
    }

    private boolean canModify(Long loginedMemberId, Reply reply) {
        return reply.getMemberId().equals(loginedMemberId);
    }

    private boolean canDelete(Long loginedMemberId, Reply reply) {
        return reply.getMemberId().equals(loginedMemberId);
    }

//    private boolean hasReaction(Long loginedMemberId, Long replyId) {
//        Long count = reactionRepository.getIsReactioned(loginedMemberId, replyId, "reply");
//        return Optional.ofNullable(count).orElse(0L) > 0;
//    }

}