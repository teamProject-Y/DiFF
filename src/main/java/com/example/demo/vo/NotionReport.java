package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotionReport {
    private Long id;
    private Long articleId;
    private String title;
    private String nickName;
    private String email;
    private String regDate;
    private String body;
    private String notionPageId;
}
