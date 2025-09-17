package com.example.demo.repository;

import com.example.demo.vo.NotionInquiry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotionInquiryRepository {

    void saveInquiry(NotionInquiry inquiry);

    void updatePageId(Long id, String notionPageId);
}
