package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DiFFApplication {

    public static void main(String[] args) {
        // 0) application.yml 존재 확인 (JAR 안)
        System.out.println("[BOOT] application.yml present? "
                + (DiFFApplication.class.getClassLoader().getResource("application.yml") != null));

        // 1) 중요 ENV 즉시 덤프 (마스킹)
        dumpEnv(
                "PORT",
                "SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD",
                "JWT_SECRET", "JWT_ACCESS_TOKEN_EXPIRATION_TIME", "JWT_REFRESH_TOKEN_EXPIRATION_TIME",
                "SONARQUBE_HOST", "SONARQUBE_TOKEN",
                "R2_endpoint", "R2_accessKey", "R2_secretKey"
        );

        SpringApplication.run(DiFFApplication.class, args);
    }

    private static void dumpEnv(String... keys) {
        for (String k : keys) {
            String v = System.getenv(k);
            System.out.println(String.format("[ENV] %-32s = %s",
                    k, maskIfSecret(k, v)));
        }
    }

    private static String maskIfSecret(String key, String val) {
        if (val == null) return "<null>";
        String k = key.toLowerCase();
        boolean secretLike = k.contains("secret") || k.contains("password") || k.contains("token") || k.contains("key");
        if (!secretLike) return val;
        int len = val.length();
        return (len <= 8) ? "******" : (val.substring(0, 4) + "..." + val.substring(len - 4));
    }
}
