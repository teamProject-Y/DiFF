package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.repository.ReactionRepository;

@Service
public class ReactionService {

    @Autowired
    private ReactionRepository reactionRepository;

    public ReactionService(ReactionRepository reactionRepository) {
        this.reactionRepository = reactionRepository;
    }


    public int like(String relType, Long relId, Long memberId) {
        return reactionRepository.like(relType, relId, memberId);
    }

    public long count(String relType, Long relId) {
        return reactionRepository.getCount(relType, relId);
    }

    public int unlike(String relType, Long relId, Long memberId) {
        return reactionRepository.unlike(relType, relId, memberId);
    }

    public boolean isLiked(String relType, Long relId, Long memberId) {
        return reactionRepository.isLiked(relType, relId, memberId);
    }
}