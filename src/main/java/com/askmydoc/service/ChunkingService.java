package com.askmydoc.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {
    private static final int CHUNK_SIZE = 200;  // WORDS
    private static final int OVERLAP = 30;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + CHUNK_SIZE, words.length);
            StringBuilder chunkBuilder = new StringBuilder();
            for (int i = start; i < end; i++) {
                chunkBuilder.append(words[i]).append(" ");
            }
            chunks.add(chunkBuilder.toString().trim());
            start += (CHUNK_SIZE - OVERLAP);
        }
        return chunks;
    }


}
