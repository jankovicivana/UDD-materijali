package com.example.ddmdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DocumentFileResponseDTO {
    private String contractServerFilename;
    private String legislationServerFilename;
}
