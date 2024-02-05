package com.example.ddmdemo.service.interfaces;

import com.example.ddmdemo.dto.DocumentResultDTO;
import com.example.ddmdemo.indexmodel.DataIndex;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface SearchService {

    List<DocumentResultDTO> simpleSearch(List<String> keywords);

    List<DocumentResultDTO> advancedSearch(List<String> expression);
}
