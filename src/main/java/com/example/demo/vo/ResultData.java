package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor

public class ResultData<DT> {

    @Getter
    private String ResultCode;
    @Getter
    private String msg;
    @Getter
    private DT data1;
    @Getter
    private String data1Name;
    @Getter
    private DT data2;
    @Getter
    private String data2Name;

    public static <DT> ResultData<DT> from(String ResultCode, String msg) {
        return from(ResultCode, msg, null, null);
    }

    public static <DT> ResultData<DT> from(String ResultCode, String msg, String data1Name, DT data) {
        ResultData<DT> rd = new ResultData<DT>();
        rd.ResultCode = ResultCode;
        rd.msg = msg;
        rd.data1Name = data1Name;
        rd.data1 = data;

        return rd;
    }

    public static <DT> ResultData<DT> from(String resultCode, String msg, String data1Name, DT data1, String data2Name,
                                           DT data2) {
        ResultData<DT> rd = new ResultData<DT>();
        rd.ResultCode = resultCode;
        rd.msg = msg;
        rd.data1Name = data1Name;
        rd.data1 = data1;
        rd.data2Name = data2Name;
        rd.data2 = data2;

        return rd;
    }

    public static <DT> ResultData<DT> newData(ResultData rd, String dataName, DT newData) {
        return from(rd.getResultCode(), rd.getMsg(), dataName, newData);
    }

    public boolean isSuccess() {
        return ResultCode.startsWith("S-");
    }

    public boolean isFail() {
        return isSuccess() == false;
    }

}