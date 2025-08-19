package com.example.demo.repository;

import com.example.demo.vo.Reply;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReplyRepository {

    public List<Reply> getReplys(Long articleId);

    public int doReplyWrtie(Long articleId, Long memberId, String body);
}