//package com.example.demo.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//@Configuration
//@EnableAsync
//public class AsyncConfig {
//
//    @Bean(name = "analysisExecutor")
//    public ThreadPoolTaskExecutor analysisExecutor() {
//        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
//        ex.setCorePoolSize(2);
//        ex.setMaxPoolSize(4);
//        ex.setQueueCapacity(50);
//        ex.setThreadNamePrefix("analysis-");
//        ex.initialize();
//        return ex;
//    }
//}
