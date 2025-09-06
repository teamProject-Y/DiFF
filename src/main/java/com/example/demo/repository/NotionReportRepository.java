package com.example.demo.repository;

import com.example.demo.vo.NotionReport;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotionReportRepository {

    void saveReport(NotionReport report);

    void updatePageId(Long id, String notionPageId);
}
