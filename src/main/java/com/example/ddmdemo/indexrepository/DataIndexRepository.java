package com.example.ddmdemo.indexrepository;

import com.example.ddmdemo.indexmodel.DataIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataIndexRepository
    extends ElasticsearchRepository<DataIndex, String> {
}
