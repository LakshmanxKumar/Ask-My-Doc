package com.askmydoc.config;

import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

import static com.askmydoc.constants.AppConstants.*;
import static com.askmydoc.constants.Prompts.QUERY_REWRITE_SYSTEM_PROMPT;
import static com.askmydoc.constants.Prompts.SYSTEM_PROMPT;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient qnaChatClient(
            Client genAiClient,
            RetryTemplate retryTemplate
    ) {

        GoogleGenAiChatModel qnaModel =
                GoogleGenAiChatModel.builder()
                        .genAiClient(genAiClient)
                        .defaultOptions(
                                GoogleGenAiChatOptions.builder()
                                        .model(QNA_MODEL)
                                        .thinkingBudget(QNA_MODEL_THINKING_BUDGET)
                                        .temperature(0.2)
                                        .build()
                        )
                        .retryTemplate(retryTemplate)
                        .observationRegistry(ObservationRegistry.NOOP)
                        .build();

        return ChatClient.builder(qnaModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Bean
    public ChatClient rewriteChatClient(
            Client genAiClient,
            RetryTemplate retryTemplate
    ) {

        GoogleGenAiChatModel rewriteModel =
                GoogleGenAiChatModel.builder()
                        .genAiClient(genAiClient)
                        .defaultOptions(
                                GoogleGenAiChatOptions.builder()
                                        .model(REWRITE_MODEL)
                                        .thinkingBudget(REWRITE_MODEL_THINKING_BUDGET)
                                        .temperature(0.1)
                                        .build()
                        )
                        .retryTemplate(retryTemplate)
                        .observationRegistry(ObservationRegistry.NOOP)
                        .build();

        return ChatClient.builder(rewriteModel)
                .defaultSystem(QUERY_REWRITE_SYSTEM_PROMPT)
                .build();
    }
}

