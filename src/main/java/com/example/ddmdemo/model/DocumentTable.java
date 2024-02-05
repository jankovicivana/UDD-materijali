package com.example.ddmdemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "document_table")
public class DocumentTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "contract_server_filename")
    private String contractServerFilename;

    @Column(name = "legislation_server_filename")
    private String legislationServerFilename;

    @Column(name = "contract_title")
    private String contractTitle;

    @Column(name = "legislation_title")
    private String legislationTitle;

    @Column(name = "contract", length = 2048)
    private String contract;

    @Column(name = "legislation", length = 2048)
    private String legislation;
}
