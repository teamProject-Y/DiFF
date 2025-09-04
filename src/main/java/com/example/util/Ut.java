package com.example.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Ut {

    public static boolean isEmpty(Object obj) {

        if(obj == null) return true;
        if(obj instanceof String) return ((String)obj).trim().length() == 0;
        if(obj instanceof Map) return ((Map<?, ?>)obj).isEmpty();
        if(obj.getClass().isArray()) return Array.getLength(obj) == 0;

        return false;
    }

    public static String f(String str, Object...args){
        return String.format(str, args);
    }

    public static Integer parseIntOrZero(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            // 소수 들어오면 반올림
            double d = Double.parseDouble(s.trim());
            return (int) Math.round(d);
        } catch (Exception ignore) {
            // 숫자만 추출 후 시도
            String digits = s.replaceAll("[^0-9.-]", "");
            if (digits.isBlank()) return 0;
            try {
                double d = Double.parseDouble(digits);
                return (int) Math.round(d);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public static Double parseDoubleOrZero(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ignore) {
            String num = s.replaceAll("[^0-9.-]", "");
            if (num.isBlank()) return 0.0;
            try { return Double.parseDouble(num); } catch (Exception e) { return 0.0; }
        }
    }

}