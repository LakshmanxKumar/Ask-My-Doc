package com.askmydoc.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class AIConfig {
//    @Bean
//    public VectorStore vectorStore(MongoTemplate mongoTemplate,
//                                   EmbeddingModel embeddingModel) {
//        return MongoDBAtlasVectorStore
//                .builder(mongoTemplate, embeddingModel)
//                .collectionName("chunks")
//                .build();
//    }
}
