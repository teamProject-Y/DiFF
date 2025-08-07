package com.example.demo.repository;

import org.apache.ibatis.annotations.Mapper;
import org.aspectj.apache.bcel.Repository;

import java.util.List;

@Mapper
public interface RepositoryRepository {
    public void makeRepository(int memberId, String repoName, String lastRqCommit);

    List<Repository> getRepositoriesByMemberId(Long memberId);
}
