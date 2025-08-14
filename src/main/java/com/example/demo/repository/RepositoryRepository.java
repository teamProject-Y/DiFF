package com.example.demo.repository;

import com.example.demo.vo.Repository;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RepositoryRepository {
    public void makeRepository(int memberId, String repoName, String lastRqCommit);

    List<com.example.demo.vo.Repository> getRepositoriesByMemberId(Long memberId);

    Repository getRepositoryByIdAndMember(Long repositoryId, Long memberId);
}
