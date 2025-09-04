package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {
    private Long id;
    private Long repositoryId;
    private Long memberId;
    private Long diffId;
    private Long articleId;

    private String projectKey;
    private String projectName;

    private Double coverage;
    private int bugs;
    private int complexity;
    private int codeSmells;
    private Double duplicatedLinesDensity;
    private int vulnerabilities;
    private LocalDateTime analyzeDate;
    private Double totalScore;
    private String checksum;

    private String gradeCoverage;
    private String gradeReliability;
    private String gradeMaintainability;
    private String gradeDuplications;
    private String gradeSecurity;
    private String gradeComplexity;
}

