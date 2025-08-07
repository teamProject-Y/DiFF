package com.example.demo.repository;

import com.example.demo.vo.Draft;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DraftRepository {

    public int getLastInsertId();

    public int existsByMemberIdAndRepoName(int memberId, String repoName);

    void insertDraft(Draft draft);
}