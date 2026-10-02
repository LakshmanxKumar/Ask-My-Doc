package com.askmydoc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService vectorSearchExecutor() {
        /*
        Vector searches are I/O-bound and spend most of their time waiting for MongoDB.
        Virtual threads are a good fit because they efficiently handle blocking I/O with
        minimal thread overhead, allowing the independent searches to run concurrently.
        */
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
