package com.example.util;

import java.util.ArrayList;
import java.util.List;

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

    private static int gradeToScore(String grade) {
        return switch (grade) {
            case "A" -> 5;
            case "B" -> 4;
            case "C" -> 3;
            case "D" -> 2;
            default -> 1; // E
        };
    }

    private static String scoreToGrade(double avg) {
        if (avg >= 4.5) return "A";
        else if (avg >= 3.5) return "B";
        else if (avg >= 2.5) return "C";
        else if (avg >= 1.5) return "D";
        return "E";
    }

    public static String totalGrade(
            String security,
            String reliability,
            String maintainability,
            String coverage,
            String duplication,
            String complexity
    ) {
        List<String> grades = new ArrayList<>();
        grades.add(security);
        grades.add(reliability);
        grades.add(maintainability);
        grades.add(coverage);
        grades.add(duplication);
        grades.add(complexity);

        int sum = 0;
        for (String g : grades) {
            sum += gradeToScore(g);
        }
        double avg = (double) sum / grades.size();

        return scoreToGrade(avg);
    }

}
