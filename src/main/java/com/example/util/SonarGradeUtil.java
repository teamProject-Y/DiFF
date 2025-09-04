package com.example.util;

public class SonarGradeUtil {

    public static String gradeSecurity(int vulnerabilities) {
        if (vulnerabilities == 0) return "A";
        else if (vulnerabilities <= 2) return "B";
        else if (vulnerabilities <= 5) return "C";
        else if (vulnerabilities <= 10) return "D";
        return "E";
    }

    public static String gradeReliability(int bugs) {
        if (bugs == 0) return "A";
        else if (bugs <= 5) return "B";
        else if (bugs <= 15) return "C";
        else if (bugs <= 30) return "D";
        return "E";
    }

    public static String gradeMaintainability(int codeSmells) {
        if (codeSmells <= 20) return "A";
        else if (codeSmells <= 100) return "B";
        else if (codeSmells <= 500) return "C";
        else if (codeSmells <= 1000) return "D";
        return "E";
    }

    public static String gradeCoverage(double coverage) {
        if (coverage >= 80) return "A";
        else if (coverage >= 60) return "B";
        else if (coverage >= 40) return "C";
        else if (coverage >= 20) return "D";
        return "E";
    }

    public static String gradeDuplications(double duplication) {
        if (duplication <= 3) return "A";
        else if (duplication <= 5) return "B";
        else if (duplication <= 10) return "C";
        else if (duplication <= 20) return "D";
        return "E";
    }

    public static String gradeComplexity(int complexity) {
        if (complexity <= 50) return "A";
        else if (complexity <= 200) return "B";
        else if (complexity <= 500) return "C";
        else if (complexity <= 1000) return "D";
        return "E";
    }

}
