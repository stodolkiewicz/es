package com.stodo.es.service.aggregation;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import com.stodo.es.document.Book;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

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
                .build();

        SearchHits<Book> bookSearchHits = esOps.search(query, Book.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) bookSearchHits.getAggregations();
        Aggregate avgRatingAggregation = aggregations.get("avgRatingAggregation").aggregation().getAggregate();

        Double avgRating = avgRatingAggregation.avg().value();

        return avgRating;
    }
}
