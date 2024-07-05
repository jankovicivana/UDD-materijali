package com.example.ddmdemo.respository;

import com.example.ddmdemo.model.DocumentTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentTable, Integer> {
}
