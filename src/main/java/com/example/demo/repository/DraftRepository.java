package com.example.demo.repository;

import com.example.demo.vo.Draft;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DraftRepository {

    public void makeRepository(int memberId, String repoName, String firstCommit);

    public int getLastInsertId();

    public int existsByMemberIdAndRepoName(int memberId, String repoName);

    public void insertDraft(Draft draft);

    public List<Draft> getDraftsByMember(Long memberId);

    public int deleteDraft(Long id, Long memberId);

    public Draft getDraftById(Long id);

    void updateDraft(Draft draft);

}