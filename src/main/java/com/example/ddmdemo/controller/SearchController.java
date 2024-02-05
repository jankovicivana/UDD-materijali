package com.example.ddmdemo.controller;

import com.example.ddmdemo.dto.AddressDTO;
import com.example.ddmdemo.dto.DocumentResultDTO;
import com.example.ddmdemo.dto.SearchQueryDTO;
import com.example.ddmdemo.indexmodel.DataIndex;
import com.example.ddmdemo.service.interfaces.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/advanced")
    public List<DocumentResultDTO> advancedSearch(@RequestBody SearchQueryDTO advancedSearchQuery) {
        return searchService.advancedSearch(advancedSearchQuery.getTokens());
    }

    @PostMapping("/address")
    public List<DocumentResultDTO> addressSearch(@RequestBody AddressDTO dto) {
        return searchService.addressSearch(dto);
    }
}
