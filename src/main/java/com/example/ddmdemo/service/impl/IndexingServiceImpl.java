package com.example.ddmdemo.service.impl;

import com.example.ddmdemo.dto.DocumentFileResponseDTO;
import com.example.ddmdemo.dto.DocumentsDTO;
import com.example.ddmdemo.exceptionhandling.exception.LoadingException;
import com.example.ddmdemo.indexmodel.DataIndex;
import com.example.ddmdemo.indexrepository.DataIndexRepository;
import com.example.ddmdemo.model.DocumentTable;
import com.example.ddmdemo.respository.DocumentRepository;
import com.example.ddmdemo.service.interfaces.FileService;
import com.example.ddmdemo.service.interfaces.IndexingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Message;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.language.detect.LanguageDetector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final DataIndexRepository dataIndexRepository;

    private final DocumentRepository dataRepository;

    private final FileService fileService;

    private final LanguageDetector languageDetector;

    private static final Logger logger = Logger.getLogger(IndexingServiceImpl.class.getName());

    @Override
    @Transactional
    public DocumentFileResponseDTO indexDocument(DocumentsDTO dto) {
        var newEntity = new DocumentTable();
        var newIndex = new DataIndex();

        var contractTitle = Objects.requireNonNull(dto.getContract().getOriginalFilename()).split("\\.")[0];
        newIndex.setContractTitle(contractTitle);
        newEntity.setContractTitle(contractTitle);

        var legislationTitle = Objects.requireNonNull(dto.getLegislation().getOriginalFilename()).split("\\.")[0];
        newIndex.setLegislationTitle(legislationTitle);
        newEntity.setLegislationTitle(legislationTitle);

        var contractContent = extractDocumentContent(dto.getContract());
        System.out.println(contractContent);
        newIndex.setContract(contractContent);
        newEntity.setContract(contractContent);

        extractData(contractContent, newIndex);

        var legislationContent = extractDocumentContent(dto.getLegislation());
        newIndex.setLegislation(legislationContent);
        newEntity.setLegislation(legislationContent);

        var contractServerFilename = fileService.store(dto.getContract(), UUID.randomUUID().toString());
        newIndex.setContractServerFilename(contractServerFilename);
        newEntity.setContractServerFilename(contractServerFilename);

        var legislationServerFilename = fileService.store(dto.getLegislation(), UUID.randomUUID().toString());
        newIndex.setLegislationServerFilename(legislationServerFilename);
        newEntity.setLegislationServerFilename(legislationServerFilename);

        var savedEntity = dataRepository.save(newEntity);

        newIndex.setDatabaseId(savedEntity.getId());
        dataIndexRepository.save(newIndex);

        logger.info(MessageFormat.format(
                "STATISTIC-LOG government_name={0};government_level={1};employee={2};city={3}",
                newIndex.getGovernmentName(), newIndex.getGovernmentLevel(), newIndex.getEmployeeName() + " " + newIndex.getEmployeeSurname(), newIndex.getAddress().split(",")[2].strip()));

        return new DocumentFileResponseDTO(contractServerFilename, legislationServerFilename);
    }

    private void extractData(String contractContent, DataIndex newIndex) {
        String governmentNameRegex = "Uprava za (\\w+)";
        String governmentLevelRegex = "nivo uprave: (\\w+)";
        String addressRegex = "(\\w+(?:\\s+\\w+)*)\\s*,\\s*(\\d+)\\s*,\\s*(\\p{L}+\\s*\\p{L}*)\\s*,\\s*u daljem tekstu klijent";
        String employeeNameSurnameRegex = "\\s+(\\p{L}+\\s+\\p{L}+)\\s*_{2,}";

        Pattern governmentNamePattern = Pattern.compile(governmentNameRegex);
        Pattern governmentLevelPattern = Pattern.compile(governmentLevelRegex);
        Pattern addressPattern = Pattern.compile(addressRegex);
        Pattern employeeNameSurnamePattern = Pattern.compile(employeeNameSurnameRegex);

        Matcher governmentNameMatcher = governmentNamePattern.matcher(contractContent);
        Matcher governmentLevelMatcher = governmentLevelPattern.matcher(contractContent);
        Matcher addressMatcher = addressPattern.matcher(contractContent);
        Matcher employeeNameSurnameMatcher = employeeNameSurnamePattern.matcher(contractContent);

        String governmentName = governmentNameMatcher.find() ? governmentNameMatcher.group(1) : "";
        String governmentLevel = governmentLevelMatcher.find() ? governmentLevelMatcher.group(1) : "";
        String address = addressMatcher.find() ? addressMatcher.group(1) + ", " + addressMatcher.group(2) + ", " + addressMatcher.group(3) : "";
        String employeeNameSurname = employeeNameSurnameMatcher.find() ? employeeNameSurnameMatcher.group(1) : "";

        String[] nameSurnameParts = employeeNameSurname.split("\\s+");
        String employeeName = nameSurnameParts.length > 0 ? nameSurnameParts[0] : "";
        String employeeSurname = nameSurnameParts.length > 1 ? nameSurnameParts[1] : "";

        System.out.println("Government Name: " + governmentName);
        System.out.println("Government Level: " + governmentLevel);
        System.out.println("Address: " + address);
        System.out.println("Employee Name: " + employeeName);
        System.out.println("Employee Surname: " + employeeSurname);

        newIndex.setGovernmentLevel(governmentLevel);
        newIndex.setGovernmentName(governmentName);
        newIndex.setEmployeeName(employeeName);
        newIndex.setEmployeeSurname(employeeSurname);

        GeoPoint geoPoint = transformAddress(address);
        newIndex.setGeopoint(geoPoint);
        newIndex.setAddress(address);
    }

    public static GeoPoint transformAddress(String address) {
        try {
            String apiUrl = "https://nominatim.openstreetmap.org/search";
            String format = "json";
            String query = address.replace(" ", "+");
            URI uri = URI.create(apiUrl + "?format=" + format + "&q=" + query);

            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.body());

            if (rootNode.isArray() && !rootNode.isEmpty()) {
                JsonNode firstResult = rootNode.get(0);
                double latitude = firstResult.get("lat").asDouble();
                double longitude = firstResult.get("lon").asDouble();

                return new GeoPoint(latitude, longitude);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String extractDocumentContent(MultipartFile multipartPdfFile) {
        String documentContent;
        try (var pdfFile = multipartPdfFile.getInputStream()) {
            var pdDocument = PDDocument.load(pdfFile);
            var textStripper = new PDFTextStripper();
            documentContent = textStripper.getText(pdDocument);
            pdDocument.close();
        } catch (IOException e) {
            throw new LoadingException("Error while trying to load PDF file content.");
        }

        return documentContent;
    }

    private String detectLanguage(String text) {
        var detectedLanguage = languageDetector.detect(text).getLanguage().toUpperCase();
        if (detectedLanguage.equals("HR")) {
            detectedLanguage = "SR";
        }

        return detectedLanguage;
    }
}