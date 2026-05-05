package com.askmydoc.controller;

import com.askmydoc.model.QueryRequest;
import com.askmydoc.service.DocumentService;
import com.askmydoc.service.QnAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/")
public class AskMyDocController {

    private final DocumentService documentService;
    private final QnAService qnaService;

    public AskMyDocController(DocumentService documentService, QnAService qnaService) {
        this.documentService = documentService;
        this.qnaService = qnaService;
    }

    @PostMapping("upload")
    public ResponseEntity<List<String>> upload(@RequestParam("file") List<MultipartFile> files) {
        if (files.isEmpty()) {
            return ResponseEntity.badRequest().body(List.of("File is empty"));
        }
        List<String> docIds = documentService.processDocument(files);
        return ResponseEntity.ok(docIds);
    }

    @PostMapping("query")
    public ResponseEntity<String> query(@RequestBody QueryRequest request) {
        return ResponseEntity.ok(qnaService.ask(request.getUserQuery(), request.getDocIds()));
    }

}
