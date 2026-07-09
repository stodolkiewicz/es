package com.stodo.es.service.aggregation;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.stodo.es.document.Book;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookAggregationService {

    private final ElasticsearchOperations esOps;

    public BookAggregationService(ElasticsearchOperations esOps) {
        this.esOps = esOps;
    }

    /*
    GET books/_search
    {
        "size": 0,
        "aggs": {
            "avgAverageRating": {
                "avg": { "field": "averageRating" }
            }
        }
    }
    */
    public Double averageRatingAggregation() {
        Aggregation avgAgg = Aggregation.of(a -> a
                .avg(avg -> avg.field("averageRating"))
        );

        NativeQuery query = NativeQuery.builder()
                .withAggregation("avgRatingAggregation", avgAgg)
                .withMaxResults(0)
                .build();

        SearchHits<Book> bookSearchHits = esOps.search(query, Book.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) bookSearchHits.getAggregations();
        Aggregate avgRatingAggregation = aggregations.get("avgRatingAggregation").aggregation().getAggregate();

        Double avgRating = avgRatingAggregation.avg().value();

        return avgRating;
    }

    /*
    Top 10 categories with book counts.

    GET books/_search
    {
        "size": 0,
        "aggs": {
            "popular_categories": {
                "terms": {
                    "field": "categories.keyword",
                    "size": 10
                }
            }
        }
    }
    */
    public TopCategories top10CategoriesAggregation() {
        Aggregation top10CategoriesAggregation = Aggregation.of(a -> a.terms(
                ta -> ta
                        .field("categories.keyword")
                        .size(10)
        ));

        NativeQuery query = NativeQuery.builder()
                .withAggregation("popularCategories", top10CategoriesAggregation)
                .withMaxResults(0)
                .build();

        SearchHits<Book> bookSearchHits = esOps.search(query, Book.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) bookSearchHits.getAggregations();
        Aggregate topCategories = aggregations.get("popularCategories").aggregation().getAggregate();

        List<StringTermsBucket> sTermsBuckets = topCategories.sterms().buckets().array();

        List<TopCategory> topCategoryList = sTermsBuckets.stream()
                .map(sTermBucket -> new TopCategory(sTermBucket.key().stringValue(), sTermBucket.docCount()))
                .toList();

        return new TopCategories(topCategoryList);
    }


}
