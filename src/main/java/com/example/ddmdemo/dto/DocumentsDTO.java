package com.example.ddmdemo.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsDTO {

    private MultipartFile file;
    private Boolean isContract;

}
