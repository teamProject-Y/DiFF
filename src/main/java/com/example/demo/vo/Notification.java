package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private Long id;
    private Long memberId;
    private String type;
    private String message;
    private boolean isRead;
    private String regDate;
}
