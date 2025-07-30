package com.example.demo.dto;

// 어느 상황에서 어떤 필드를 검증할지 구분
public interface ValidationGroups {
    interface Join {} // 가입
    interface Modify {} // 수정
}
