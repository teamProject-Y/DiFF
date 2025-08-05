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

    private Long memberId;

    private String projectKey;
    private String projectName;

    private Double coverage;                 // 커버리지
    private int bugs;                    // 버그 수
    private int complexity;              // 복잡도
    private int codeSmells;              // 코드 스멜 수
    private Double duplicatedLinesDensity;   // 중복 비율
    private int vulnerabilities;         // 보안 취약점 수
    private LocalDateTime analyzeDate;       // 분석 일자
    private Double totalScore;
}
