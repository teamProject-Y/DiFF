package com.example.demo.repository;

import com.example.demo.vo.Draft;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DraftRepository {

    public void makeRepository(int memberId, String repoName, String firstCommit);

    public int getLastInsertId();

    public int existsByMemberIdAndRepoName(int memberId, String repoName);

    void insertDraft(Draft draft);

    List<Draft> getDraftsByMember(Long memberId);
}