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
public class Commit {
    private String sha;
    private String message;
    private String htmlUrl;
    private String parentSha;

    private String AuthorName;
    private String AuthoredAt;
    private String AuthorLogin;
}
