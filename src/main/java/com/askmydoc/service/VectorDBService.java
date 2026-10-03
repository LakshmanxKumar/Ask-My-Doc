package com.askmydoc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.askmydoc.constants.AppConstants.DB_TOP_K;

@Service
public class VectorDBService {
    private static final Logger logger = LoggerFactory.getLogger(VectorDBService.class);
    private final VectorStore vectorStore;

    public VectorDBService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocuments(List<Document> documents) {
        vectorStore.add(documents);
    }

    public List<Document> searchSimilarDocuments(String query, List<String> docIds) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(DB_TOP_K)
                .filterExpression(buildFilterExpression(docIds))
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    public boolean deleteByDocIds(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("At least one docId must be provided");
        }
        try {
            vectorStore.delete(buildFilterExpression(docIds));
            logger.info("Deleted all documents with docIds: {}", docIds);
            return true;
        } catch (Exception e) {
            logger.error("Failed to delete documents with docIds: {}", docIds, e);
            return false;
        }
    }

    private Filter.Expression buildFilterExpression(List<String> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("At least one docId must be provided");
        }
        return new FilterExpressionBuilder()
                .in("docId", docIds.toArray())
                .build();
    }
}
