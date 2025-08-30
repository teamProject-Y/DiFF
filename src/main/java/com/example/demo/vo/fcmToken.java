package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class fcmToken {
    private Long Id;
    private Long memberId;
    private String fcmToken;
    private String regDate;
}
