package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.DocumentFileResponseDTO;
import com.example.ddmdemo.dto.DocumentsDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface IndexingService {

    DocumentFileResponseDTO indexDocument(DocumentsDTO dto);
}
