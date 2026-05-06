package com.askmydoc.service;


import com.askmydoc.model.CohereResponse;
import com.askmydoc.model.CohereResult;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.askmydoc.constants.AppConstants.*;

@Service
public class RerankerService {
    private final RestClient client;

    @Value("${COHERE_API_KEY}")
    private String apiKey;

    public RerankerService(RestClient.Builder builder) {
        client = builder.baseUrl(COHERE_BASE_URL).defaultHeader(AUTHORIZATION, getToken()).build();
    }

    public List<Document> reRankDocs(List<Document> chunks, String query) {

        // 1. Extract text from documents
        List<String> texts = chunks.stream()
                .map(Document::getText)
                .toList();

        // 2. Build request
        Map<String, Object> requestBody = Map.of(
                "model", RERANKER_MODEL,
                "query", query,
                "documents", texts,
                "top_n", RERANKED_TOP_N
        );

        // 3. Call Cohere
        CohereResponse response = client.post()
                .uri(COHERE_RERANKED_ENDPOINT)
                .body(requestBody)
                .retrieve()
                .body(CohereResponse.class);

        if (response == null || response.getResults() == null) {
            return Collections.emptyList();
        }

        // 4. Map results back to original Document objects
        return response.getResults().stream()
                .sorted(Comparator.comparing(CohereResult::getRelevanceScore).reversed())
                .map(result -> chunks.get(result.getIndex()))
                .toList();
    }

    public String getToken() {
        return "Bearer " + apiKey;
    }
}
