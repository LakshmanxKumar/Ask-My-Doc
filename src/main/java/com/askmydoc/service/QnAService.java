package com.askmydoc.service;

import com.askmydoc.model.ModelResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class QnAService {

    private final ChatClient chatClient;

    private final VectorStore vectorStore;

    public QnAService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a helpful assistant.
                        You must answer ONLY using the provided context.
                        
                        For every answer:
                        - Include a supporting quote from the context.
                        - The quote must be copied EXACTLY from the context.
                        - Do NOT paraphrase the quote.
                        
                        If the answer cannot be supported by the context:
                        - Respond: "I don't have enough information to answer."
                        
                        Do NOT use prior knowledge.
                        Do NOT generate answers without a supporting quote.
                        """)
                .build();
        this.vectorStore = vectorStore;
    }

    public String ask(String ques, List<String> docIds) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(ques)
                .topK(4)
                .filterExpression(buildFilterExpression(docIds))
                .build();

        List<Document> searchResult = vectorStore.similaritySearch(searchRequest);

        String context = IntStream.range(0, searchResult.size())
                .mapToObj(i -> {
                    Document d = searchResult.get(i);
                    return "[Chunk " + i + "]: " + d.getText();
                })
                .collect(Collectors.joining("\n\n"));

        String userPrompt = """
                You are given a context and a question.
                
                Follow these rules strictly:
                - Answer ONLY using the provided context.
                - Provide one supporting quote copied EXACTLY from the context.
                - Do NOT paraphrase the quote.
                - If the answer is not present, set:
                  answer = "I don't have enough information to answer."
                  support = ""
                
                Return ONLY valid JSON in the following format:
                {
                  "answer": "...",
                  "support": "..."
                }
                
                Do NOT include any explanation, text, or markdown outside the JSON.
                
                Context:
                %s
                
                Question:
                %s
                """.formatted(context, ques);

        ModelResponse modelResponse = chatClient.prompt()
                .user(userPrompt)
                .call()
                .entity(ModelResponse.class);

        return getValidatedResponse(modelResponse, searchResult);
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

        return docIds.stream()
                .map(id -> "docId == '" + id + "'")
                .collect(Collectors.joining(" OR "));
    }
}
