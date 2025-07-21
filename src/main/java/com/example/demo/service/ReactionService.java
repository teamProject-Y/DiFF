package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ReactionRepository;
import com.example.demo.vo.ResultData;

import util.Ut;

@Service
public class ReactionService {

    @Autowired
    private ReactionRepository reactionRepository;

    public ReactionService(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }

    // article Service에 중복됨 이 메서드는 사용중 X
//    private ResultData userReaction(int loginedMemberId, int id, String relTypecode) {
//
//        int reactionPoint = reactionRepository.getUserReaction(loginedMemberId, id, relTypecode);
//
//        if(reactionPoint == 0) return ResultData.from("F-1",Ut.f("%d번 게시글 반응", id), "없음", reactionPoint);
//        else if(reactionPoint == 1) return ResultData.from("S-1",Ut.f("%d번 게시글 반응", id), "좋아요", reactionPoint);
//        else return ResultData.from("F-A",Ut.f("%d번 게시글 반응", id), "싫어요", reactionPoint);
//
//    }
//
//    public void doGoodReaction(int loginedMemberId, int id, String relTypecode) {
//        int affectedRow = reactionRepository.doGoodReaction(loginedMemberId, id, relTypecode);
//    }
//
//    public void doChangeReaction(int loginedMemberId, int id, int point, String relTypecode) {
//        int affectedRow = reactionRepository.doChangeReaction(loginedMemberId, id, point, relTypecode);
//    }
}