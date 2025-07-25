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


}