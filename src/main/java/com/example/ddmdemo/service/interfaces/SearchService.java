package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.AddressDTO;
import com.example.ddmdemo.dto.DocumentResultDTO;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {

    List<DocumentResultDTO> simpleSearch(List<String> keywords);

    List<DocumentResultDTO> advancedSearch(List<String> expression);

    List<DocumentResultDTO> addressSearch(AddressDTO dto);
}
