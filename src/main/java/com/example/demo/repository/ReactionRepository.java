package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ReactionRepository {

    public Long getUserReaction(Long loginedMemberId, Long id, String relTypeCode);

    public Long getIsReactioned(Long loginedMemberId, Long id, String relTypeCode);
}