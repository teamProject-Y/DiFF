package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotionInquiry {
    private Long id;
    private String title;
    private String nickName;
    private String email;
    private String regDate;
    private String body;
    private String notionPageId;
}
