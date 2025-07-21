package com.example.demo.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.demo.vo.Comment;

@Mapper
public interface CommentRepository {

    public List<Comment> getComments(int relId);

    public int doCommentWrtie(String relTypeCode, int relId, int memberId, String body);

}