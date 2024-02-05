package com.example.ddmdemo.indexmodel;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "document_index")
@Setting(settingPath = "/configuration/serbian-analyzer-config.json")
public class DataIndex {

    @Id
    private String id;

    @Field(type = FieldType.Integer, store = true, name = "database_id")
    private Integer databaseId;

    @Field(type = FieldType.Text, store = true, name = "contract_title")
    private String contractTitle;

    @Field(type = FieldType.Text, store = true, name = "legislation_title")
    private String legislationTitle;

    @Field(type = FieldType.Text, store = true, name = "contract_server_filename")
    private String contractServerFilename;

    @Field(type = FieldType.Text, store = true, name = "legislation_server_filename")
    private String legislationServerFilename;

    @Field(type = FieldType.Text, store = true, name = "contract", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String contract;

    @Field(type = FieldType.Text, store = true, name = "legislation", analyzer = "serbian_simple", searchAnalyzer = "serbian_simple")
    private String legislation;

    @Field(type = FieldType.Text, store = true, name = "government_name")
    private String governmentName;

    @Field(type = FieldType.Text, store = true, name = "government_level")
    private String governmentLevel;

    @Field(type = FieldType.Text, store = true, name = "employee_name")
    private String employeeName;

    @Field(type = FieldType.Text, store = true, name = "employee_surname")
    private String employeeSurname;

    @Field(type = FieldType.Text, store = true, name = "address")
    private String address;

    @GeoPointField
    @Field(store = true, name = "geopoint")
    private GeoPoint geopoint;
}
