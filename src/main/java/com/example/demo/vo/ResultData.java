package com.example.demo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class ResultData<DT> {

    @Getter
    @JsonProperty("resultCode")
    private String ResultCode;
    @Getter
    private String msg;

    // 1개/2개 데이터를 유연히 담기 위한 필드
    @Getter
    private DT data1;
    @Getter
    private String data1Name;
    @Getter
    private DT data2;
    @Getter
    private String data2Name;

    /* ---------- 팩토리 메서드들 ---------- */

    // (A) 메시지만
    public static <DT> ResultData<DT> from(String resultCode, String msg) {
        return from(resultCode, msg, null, null);
    }

    // (B) "이름 없는" 단일 데이터: 편의 오버로드
    public static <DT> ResultData<DT> from(String resultCode, String msg, DT data) {
        ResultData<DT> rd = new ResultData<>();
        rd.ResultCode = resultCode;
        rd.msg = msg;
        rd.data1Name = "data";   // 기본 키 이름
        rd.data1 = data;
        return rd;
    }

    // (C) 이름 있는 단일 데이터
    public static <DT> ResultData<DT> from(String resultCode, String msg, String data1Name, DT data1) {
        ResultData<DT> rd = new ResultData<>();
        rd.ResultCode = resultCode;
        rd.msg = msg;
        rd.data1Name = data1Name;
        rd.data1 = data1;
        return rd;
    }

    // (D) 이름 있는 두 개의 데이터
    public static <DT> ResultData<DT> from(String resultCode, String msg,
                                           String data1Name, DT data1,
                                           String data2Name, DT data2) {
        ResultData<DT> rd = new ResultData<>();
        rd.ResultCode = resultCode;
        rd.msg = msg;
        rd.data1Name = data1Name;
        rd.data1 = data1;
        rd.data2Name = data2Name;
        rd.data2 = data2;
        return rd;
    }

    // 기존 rd에서 데이터만 교체해서 새로 만들 때
    public static <DT> ResultData<DT> newData(ResultData<?> rd, String dataName, DT newData) {
        return from(rd.getResultCode(), rd.getMsg(), dataName, newData);
    }

    /* ---------- 편의 메서드 ---------- */

    public boolean isSuccess() {
        return ResultCode != null && ResultCode.startsWith("S-");
    }

    public boolean isFail() {
        return !isSuccess();
    }

    // data1를 기본 데이터로 노출
    public DT getData() {
        return data1;
    }
}
