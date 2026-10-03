package com.askmydoc.service;

import com.askmydoc.model.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.askmydoc.constants.AppConstants.DB_TOP_K;
import static com.askmydoc.constants.AppConstants.RERANKED_TOP_N;
import static com.askmydoc.constants.Prompts.USER_PROMPT_TEMPLATE;

@Service
public class QnAService {

    private final ChatClient qnAChatClient;
    private final VectorStore vectorStore;
    private final RerankerService rerankerService;

    private static final Logger logger = LoggerFactory.getLogger(QnAService.class);

    public QnAService(@Qualifier("qnaChatClient") ChatClient qnAChatClient,
                      VectorStore vectorStore,
                      RerankerService rerankerService) {
        this.qnAChatClient = qnAChatClient;
        this.vectorStore = vectorStore;
        this.rerankerService = rerankerService;
    }

    public String ask(String ques, List<String> docIds) {

        List<Document> reRankedResult = getRerankedSearchResults(ques, docIds);
        if (reRankedResult.isEmpty()) {
            return "Error while fetching";
        }
        String context = IntStream.range(0, reRankedResult.size())
                .mapToObj(i -> {
                    Document d = reRankedResult.get(i);
                    return "[Chunk " + i + "]: " + d.getText();
                })
                .collect(Collectors.joining("\n\n"));

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(context, ques);

        ModelResponse modelResponse = qnAChatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(ModelResponse.class);

        return getValidatedResponse(modelResponse, reRankedResult);
    }

    private List<Document> getRerankedSearchResults(
            String ques, List<String> docIds) {
        ques = ques.toLowerCase();

        List<Document> searchResults = searchAndDeduplicate(ques, docIds);

        if (searchResults.isEmpty()) {
            logger.error("No Chunks found");
            return Collections.emptyList();
        }
        if (searchResults.size() > RERANKED_TOP_N) {
            return rerankerService.reRankDocs(
                    searchResults,
                    ques
            );
        }
        // if we don't have enough results, no need to rerank
        return searchResults;
    }


    private List<Document> searchAndDeduplicate(
            String query,
            List<String> docIds) {

        List<Document> documentList = searchSimilarDocuments(query, docIds);

        Map<String, Document> chunks = new LinkedHashMap<>();
        for (Document doc : documentList) {
            String uniqueKey =
                    doc.getMetadata().get("docId") + ":" + doc.getMetadata().get("chunkIndex");
            chunks.putIfAbsent(uniqueKey, doc);
        }

        return new ArrayList<>(chunks.values());
    }

    private List<Document> searchSimilarDocuments(String query, List<String> docIds) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(DB_TOP_K)
                .filterExpression(buildFilterExpression(docIds))
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    private String getValidatedResponse(ModelResponse response, List<Document> docs) {

        if (response == null
                || response.getSupport() == null
                || response.getSupport().isBlank()) {
            return "I don't have enough information to answer.";
        }

        String support = normalize(response.getSupport());

        for (Document doc : docs) {
            String chunk = normalize(doc.getText());

            if (calculateSimilarity(support, chunk) > 0.7) {
                return getFormattedResponse(response);
            }
        }

        return "I don't have enough information to answer.";
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String getFormattedResponse(ModelResponse response) {
        return """
                Answer: %s
                
                Source: "%s"
                
                """.formatted(
                response.getAnswer(),
                response.getSupport()
        );
    }

    private double calculateSimilarity(String a, String b) {
        Set<String> wordsA = new HashSet<>(Arrays.asList(a.split("\\s+")));
        Set<String> wordsB = new HashSet<>(Arrays.asList(b.split("\\s+")));

        int intersection = 0;

        for (String word : wordsA) {
            if (wordsB.contains(word)) {
                intersection++;
            }
        }

        return (double) intersection / wordsA.size();
    }

    private String buildFilterExpression(List<String> docIds) {

        if (docIds == null || docIds.isEmpty()) {
            throw new IllegalArgumentException("At least one docId must be provided");
        }

        String ids = docIds.stream()
                .map(id -> "'" + id + "'")
                .collect(Collectors.joining(", "));

        return "docId in [" + ids + "]";
    }
}
