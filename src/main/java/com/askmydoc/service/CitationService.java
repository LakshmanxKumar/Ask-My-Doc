package com.askmydoc.service;

import com.askmydoc.model.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.askmydoc.constants.AppConstants.NOT_ENOUGH_INFO_MSG;

@Service
public class CitationService {
    private static final Logger logger = LoggerFactory.getLogger(CitationService.class);

    public String getValidatedResponse(ModelResponse response, List<Document> docs) {

        if (response == null
                || response.getSupport() == null
                || response.getSupport().isBlank()) {
            return NOT_ENOUGH_INFO_MSG;
        }

        String support = normalize(response.getSupport());

        for (Document doc : docs) {
            String chunk = normalize(doc.getText());
            double score = calculateSimilarity(support, chunk);
            if (score > 0.7) {
                return getFormattedResponse(response);
            }
        }
        logger.info(response.getAnswer());
        logger.warn("Failed at citation validation");
        return NOT_ENOUGH_INFO_MSG;
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
}
