package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private String AuthorAvatarUrl;
    private String AuthorLogin;

    private Map<String,Object> stats;
    private List<Map<String,Object>> files;
}
