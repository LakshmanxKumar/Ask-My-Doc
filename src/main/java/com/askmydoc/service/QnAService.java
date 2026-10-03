package com.askmydoc.service;

import com.askmydoc.model.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.askmydoc.constants.AppConstants.*;
import static com.askmydoc.constants.Prompts.USER_PROMPT_TEMPLATE;

@Service
public class QnAService {

    private static final Logger logger = LoggerFactory.getLogger(QnAService.class);

    private final ChatClient qnAChatClient;
    private final RerankerService rerankerService;
    private final VectorDBService vectorDBService;
    private final CitationService citationService;

    public QnAService(@Qualifier("qnaChatClient") ChatClient qnAChatClient,
                      RerankerService rerankerService,
                      VectorDBService vectorDBService,
                      CitationService citationService) {
        this.qnAChatClient = qnAChatClient;
        this.rerankerService = rerankerService;
        this.vectorDBService = vectorDBService;
        this.citationService = citationService;
    }

    public boolean deleteByDocIds(List<String> docIds) {
        return vectorDBService.deleteByDocIds(docIds);
    }

    public String ask(String ques, List<String> docIds) {
        List<Document> reRankedResult = getRerankedSearchResults(ques, docIds);
        if (reRankedResult.isEmpty()) {
            return "Error while fetching";
        }
        String context = prepareContext(reRankedResult);

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(context, ques);

        ModelResponse modelResponse = qnAChatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(ModelResponse.class);

        if (NOT_ENOUGH_INFO_MSG.equalsIgnoreCase(modelResponse.getAnswer())) {
            logger.warn("LLM response is negative");
            return NOT_ENOUGH_INFO_MSG;
        }
        return citationService.getValidatedResponse(modelResponse, reRankedResult);
    }


    private static String prepareContext(List<Document> reRankedResult) {
        return IntStream.range(0, reRankedResult.size())
                .mapToObj(i -> {
                    Document d = reRankedResult.get(i);
                    return "[Chunk " + i + "]: " + d.getText();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private List<Document> getRerankedSearchResults(
            String ques, List<String> docIds) {
        ques = ques.toLowerCase();

        List<Document> searchResults = collectSimilarDocuments(ques, docIds);

        if (searchResults.isEmpty()) {
            logger.error("No Chunks found");
            return Collections.emptyList();
        }
        if (searchResults.size() > RERANKED_TOP_N) {
            return rerankerService.reRankDocs(searchResults, ques);
        }
        // if we don't have enough results, no need to rerank
        return searchResults;
    }

    private List<Document> collectSimilarDocuments(
            String query,
            List<String> docIds) {
        List<Document> documentList = vectorDBService.searchSimilarDocuments(query, docIds);

        Map<String, Document> chunks = new LinkedHashMap<>();
        for (Document doc : documentList) {
            String uniqueKey =
                    doc.getMetadata().get("docId") + ":" + doc.getMetadata().get("chunkIndex");
            chunks.putIfAbsent(uniqueKey, doc);
        }
        return new ArrayList<>(chunks.values());
    }
}
