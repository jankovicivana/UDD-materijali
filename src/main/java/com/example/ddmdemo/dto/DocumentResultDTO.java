package com.example.ddmdemo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResultDTO {
    private String id;
    private String title;
    private String filename;
    private String governmentName;
    private String governmentLevel;
    private String employeeName;
    private String employeeSurname;
    private String address;
    private String highlights;
}
