package com.example.demo.repository;

import com.example.demo.vo.Reply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReplyRepository {

    List<Reply> getReplies(Long articleId);

    int doReplyWrtie(
            @Param("articleId") Long articleId,
            @Param("memberId") Long memberId,
            @Param("body") String body
    );

    Reply getReplyById(Long id);

    int deleteReply(Long id, Long memberId);

    int modifyReply(Reply reply);
}