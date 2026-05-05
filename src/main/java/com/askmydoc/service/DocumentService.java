package com.askmydoc.service;

import com.askmydoc.exceptions.ParsingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final ParsingService parsingService;
    private final ChunkingService chunkingService;
    private final VectorStore vectorStore;

    public DocumentService(ParsingService parsingService, ChunkingService chunkingService,
                           VectorStore vectorStore) {
        this.parsingService = parsingService;
        this.chunkingService = chunkingService;
        this.vectorStore = vectorStore;
    }

    public List<String> processDocument(List<MultipartFile> files) {
        List<String> processedDocIds = new ArrayList<>();
        for (MultipartFile file : files) {
            String id = processSingleDocument(file);
            if (id != null) {
                processedDocIds.add(id);
            }
        }
        return processedDocIds;
    }

    private String processSingleDocument(MultipartFile file) {
        try {
            String text = parsingService.parse(file);
            List<String> chunks = chunkingService.chunk(text);
            List<Document> docs = new ArrayList<>();
            String docId = UUID.randomUUID().toString();
            for (int i = 0; i < chunks.size(); i++) {
                docs.add(createDocument(chunks.get(i), docId, i, file.getOriginalFilename()));
            }
            vectorStore.add(docs);
            return docId;
        } catch (ParsingFailureException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    private Document createDocument(String chunk, String docId,
                                    int chunkIndex, String fileName) {
        return new Document(chunk,
                Map.of("docId", docId,
                        "chunkIndex", chunkIndex,
                        "fileName", fileName));
    }
}
