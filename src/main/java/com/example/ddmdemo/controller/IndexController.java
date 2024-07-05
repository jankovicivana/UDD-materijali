package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.DocumentsDTO;
import com.example.ddmdemo.dto.DocumentFileResponseDTO;
import com.example.ddmdemo.service.interfaces.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class IndexController {

    private final IndexingService indexingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentFileResponseDTO addDocumentFile(DocumentsDTO documents) {
        return indexingService.indexDocument(documents);
    }
}
