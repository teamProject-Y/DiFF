package com.example.demo.repository;

import com.example.demo.vo.Diff;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiffRepository {
    void updateDiff(Diff diff);

    void insertDiff(Diff diff);
}
