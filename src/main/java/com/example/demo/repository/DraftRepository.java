package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DraftRepository {

    public void makeRepository(int memberId, String repoName, String firstCommit);

    public int getLastInsertId();

    public int existsByMemberIdAndRepoName(int memberId, String repoName);
}