package com.example.demo.repository;

import com.example.demo.vo.NotionInquiry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotionInquiryRepository {
    public void saveInquiry(NotionInquiry inquiry);

    public void updatePageId(Long id, String notionPageId);
}
