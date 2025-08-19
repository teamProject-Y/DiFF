package com.example.demo.repository;

import com.example.demo.vo.Reply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReplyRepository {

    public List<Reply> getReplies(Long articleId);

    public int doReplyWrtie(
            @Param("articleId") Long articleId,
            @Param("memberId") Long memberId,
            @Param("body") String body
    );

    public Reply getReplyById(Long id);

    public int deleteReply(Long id, Long memberId);

    public int modifyReply(Reply reply);
}