package com.askmydoc.service;

import com.askmydoc.model.ModelResponse;
import com.askmydoc.model.QueryRewriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.askmydoc.constants.AppConstants.DB_TOP_K;
import static com.askmydoc.constants.Prompts.USER_PROMPT_TEMPLATE;

@Service
public class QnAService {

    private final ChatClient qnAChatClient;
    private final ChatClient rewriteChatClient;
    private final VectorStore vectorStore;
    private final RerankerService rerankerService;
    private final ExecutorService vectorSearchExecutor;

    private static final Logger logger = LoggerFactory.getLogger(QnAService.class);

    public QnAService(@Qualifier("qnaChatClient") ChatClient qnAChatClient,
                      @Qualifier("rewriteChatClient") ChatClient rewriteChatClient, VectorStore vectorStore,
                      RerankerService rerankerService, ExecutorService vectorSearchExecutor) {
        this.qnAChatClient = qnAChatClient;
        this.vectorStore = vectorStore;
        this.rerankerService = rerankerService;
        this.rewriteChatClient = rewriteChatClient;
        this.vectorSearchExecutor = vectorSearchExecutor;

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

        QueryRewriteResponse qrr = rewriteQuery(ques);

        List<Document> uniqueChunks = searchAndDeduplicate(qrr, docIds);

        if (uniqueChunks.isEmpty()) {
            logger.error("No Chunks found");
            return Collections.emptyList();
        }

        return rerankerService.reRankDocs(
                uniqueChunks,
                qrr.getCorrectedQuery().toLowerCase()
        );
    }

    private QueryRewriteResponse rewriteQuery(String ques) {

        String userPrompt = """
                Rewrite the following query for retrieval:
                
                %s
                """.formatted(ques);

        return rewriteChatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(QueryRewriteResponse.class);
    }

    private List<Document> searchAndDeduplicate(
            QueryRewriteResponse qrr,
            List<String> docIds) {

        List<String> queries = List.of(
                qrr.getCorrectedQuery().toLowerCase(),
                qrr.getRephrasedQueries().getFirst().toLowerCase(),
                qrr.getRephrasedQueries().getLast().toLowerCase()
        );

        List<CompletableFuture<List<Document>>> futures = queries.stream()
                .map(query -> CompletableFuture.supplyAsync(
                        () -> searchSimilarDocuments(query, docIds),
                        vectorSearchExecutor
                ))
                .toList();

        Map<String, Document> uniqueChunks = new LinkedHashMap<>();

        for (CompletableFuture<List<Document>> future : futures) {
            for (Document doc : future.join()) {

                String uniqueKey =
                        doc.getMetadata().get("docId")
                                + ":"
                                + doc.getMetadata().get("chunkIndex");

                uniqueChunks.putIfAbsent(uniqueKey, doc);
            }
        }

        return new ArrayList<>(uniqueChunks.values());
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
