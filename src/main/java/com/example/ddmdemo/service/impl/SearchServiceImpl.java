package com.example.ddmdemo.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.ddmdemo.dto.AddressDTO;
import com.example.ddmdemo.dto.DocumentResultDTO;
import com.example.ddmdemo.indexmodel.DataIndex;
import com.example.ddmdemo.service.interfaces.SearchService;

import java.util.*;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.common.unit.Fuzziness;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchTemplate;

    @Override
    public List<DocumentResultDTO> simpleSearch(List<String> tokens) {
        var searchQueryBuilder = new NativeQueryBuilder().withQuery(buildSimpleSearchQuery(tokens));
        return runQuery(searchQueryBuilder.build(), "content");
    }

    private Query buildSimpleSearchQuery(List<String> tokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            tokens.forEach(token -> {
                b.should(sb -> sb.match(
                        m -> m.field("title").fuzziness(Fuzziness.ONE.asString()).query(token)));
                b.should(sb -> sb.match(m -> m.field("content_sr").query(token)));
                b.should(sb -> sb.match(m -> m.field("content_en").query(token)));
            });
            return b;
        })))._toQuery();
    }

    @Override
    public List<DocumentResultDTO> advancedSearch(List<String> tokens) {
        List<String> postfix = toPostfix(tokens);
        Query builder = buildAdvancedSearchQuery(postfix);
        var searchQuery = new NativeQueryBuilder()
                .withQuery(builder)
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(List.of(
                                new HighlightField("content"),
                                new HighlightField("address")
                        )), DataIndex.class))
                .build();

        return runQuery(searchQuery, "content");

    }

    @Override
    public List<DocumentResultDTO> addressSearch(AddressDTO dto){
        GeoPoint point = IndexingServiceImpl.transformAddress(dto.getAddress());

        Query query = BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            b.filter(g -> g.geoDistance(gg -> gg.field("geopoint").distance(dto.getDistance()+"km")
                    .location(geoLocation -> geoLocation
                            .latlon(latLonGeoLocation -> latLonGeoLocation
                                    .lon(point.getLon()).lat(point.getLat())))));
            return b;
        })))._toQuery();

        var searchQuery = new NativeQueryBuilder()
                .withQuery(query)
                .withHighlightQuery(new HighlightQuery(
                        new Highlight(List.of(
                                new HighlightField("address")
                        )), DataIndex.class))
                .build();

        return runQuery(searchQuery, "address");
    }


    private List<DocumentResultDTO> runQuery(NativeQuery searchQuery, String highlightField) {
        SearchHits<DataIndex> hits = elasticsearchTemplate.search(searchQuery, DataIndex.class,
            IndexCoordinates.of("document_index"));
        List<DocumentResultDTO> results = new ArrayList<>();
        for (SearchHit<DataIndex> hit : hits) {
            var data = hit.getContent();

            var highlight = hit.getHighlightFields().get(highlightField);
            String h = "";
            if (highlight != null) {
                h = String.join("  ...  ", highlight);
                h = h.replace("<em>", "<em><b>").replace("</em>", "</b></em>");
            }

            results.add(new DocumentResultDTO(
                    data.getId(),
                    data.getTitle(),
                    data.getServerFilename(),
                    data.getGovernmentName(),
                    data.getGovernmentLevel(),
                    data.getEmployeeName(),
                    data.getEmployeeSurname(),
                    data.getAddress(),
                    h));
        }

        return results;
    }

    public static List<String> toPostfix(List<String> tokens) {
        List<String> postfix = new ArrayList<>();
        Stack<String> operatorStack = new Stack<>();

        for (String token : tokens) {
            switch (token) {
                case "not":
                    operatorStack.push(token);
                    break;
                case "and", "or":
                    while (!operatorStack.isEmpty() && hasHigherPrecedence(operatorStack.peek(), token)) {
                        postfix.add(operatorStack.pop());
                    }
                    operatorStack.push(token);
                    break;
                default:
                    postfix.add(token);
                    break;
            }
        }

        while (!operatorStack.isEmpty()) {
            postfix.add(operatorStack.pop());
        }

        return postfix;
    }

    private static boolean hasHigherPrecedence(String op1, String op2) {
        return getPrecedence(op1) >= getPrecedence(op2);
    }

    private static int getPrecedence(String operator) {
        switch (operator) {
            case "not":
                return 3;
            case "and":
                return 2;
            case "or":
                return 1;
            default:
                return 0;
        }
    }


    public static Query buildAdvancedSearchQuery(List<String> postfixTokens) {
        return BoolQuery.of(q -> q.must(mb -> mb.bool(b -> {
            parsePostfixExpression(postfixTokens, b);
            return b;
        })))._toQuery();
    }

    public static void parsePostfixExpression(List<String> postfixTokens, BoolQuery.Builder b) {
        var operandStack = new Stack<String>();
//        var builders = new Stack<Query>();
        for (var token : postfixTokens) {
            if (isOperator(token)) {
                var value = operandStack.pop();
                var field = operandStack.pop();

                switch (token.strip().toLowerCase()) {
                    case "not":
                        b.mustNot(sb -> sb.match(m -> m.field(field).fuzziness(Fuzziness.ONE.asString()).query(value)));
                        break;
                    case "and":
                        var value1 = operandStack.pop();
                        var field1 = operandStack.pop();
//                        Query qq = BoolQuery.of(q -> q.must(mb -> mb.bool(bb -> {
//                            parsePostfixExpression(postfixTokens, bb);
//                            return bb;
//                        })))._toQuery();
//                        b.must(qq);

                        b.must(q -> q.bool(mb ->
                                {
                                    mb.must(sb -> {
                                        if (!value.contains(" "))
                                            sb.match(m -> m.field(field).fuzziness(Fuzziness.ONE.asString()).query(value));
                                        else
                                            sb.matchPhrase(m -> m.field(field).query(value));
                                        return sb;
                                    });
                                    mb.must(sb ->
                                    {
                                        if (!value.contains(" "))
                                            sb.match(m -> m.field(field1).fuzziness(Fuzziness.ONE.asString()).query(value1));
                                        else
                                            sb.matchPhrase(m -> m.field(field1).query(value1));
                                        return sb;
                                    });
                                    return mb;
                                }
                        ));
                        break;
                    case "or":
                        var value2 = operandStack.pop();
                        var field2 = operandStack.pop();
                        b.should(sb -> sb.match(m -> m.field(field).fuzziness(Fuzziness.ONE.asString()).query(value)));
                        b.should(sb -> sb.match(m -> m.field(field2).fuzziness(Fuzziness.ONE.asString()).query(value2)));
                        b.minimumShouldMatch("1");
                        break;
                    default:
                        System.out.println(token);
                }
            } else {
                operandStack.push(token);
            }
        }
//
//        b.must(builders.pop());
    }


    private static boolean isOperator(String input) {
        return switch (input.strip().toLowerCase()) {
            case "not", "and", "or" -> true;
            default -> false;
        };
    }

}
