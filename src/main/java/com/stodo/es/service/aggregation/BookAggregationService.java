package com.stodo.es.service.aggregation;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.util.NamedValue;
import com.stodo.es.service.aggregation.TopCategoriesWithAvgRating.CategoryAvgRating;
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

    /*
    Note on sterms():
    In the JSON response a terms aggregation always looks the same, but bucket keys
    have different types depending on the field. Java is strongly typed, so the ES
    client splits this into separate variants of the Aggregate union:
      - sterms() — string keys (our case: categories.keyword)
      - lterms() — long keys (e.g. terms on publishedYear)
      - dterms() — double keys
    Picking the wrong variant fails at runtime, not at compile time.
    */


    /*
    Average rating inside each of the 10 most frequent categories,
    buckets sorted by that average.

    Aggregations run on the documents matched by the query. The "exists" query
    keeps only books that have averageRating, so every bucket's avg is non-null.

    GET books/_search
    {
        "size": 0,
        "query": {
            "exists": { "field": "averageRating" }
        },
        "aggs": {
            "popular_categories": {
                "terms": {
                    "field": "categories.keyword",
                    "size": 10,
                    "order": { "averageRating": "desc" }
                },
                "aggs": {
                    "averageRating": {
                        "avg": { "field": "averageRating" }
                    }
                }
            }
        }
    }
    */
    public TopCategoriesWithAvgRating top10CategoriesWithAvgRatingsAggregation() {
        Aggregation avgAgg = Aggregation.of(a -> a
                .avg(avg -> avg.field("averageRating"))
        );

        Aggregation top10CategoriesWithAvgRatingAggregation = Aggregation.of(a -> a
                .terms(
                ta -> ta
                        .field("categories.keyword")
                        .size(10)
                        .order(NamedValue.of("averageRating", SortOrder.Desc))
                        )
                .aggregations("averageRating", avgAgg)
        );

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.exists(e -> e.field("averageRating"))))
                .withAggregation("popularCategories", top10CategoriesWithAvgRatingAggregation)
                .withMaxResults(0)
                .build();

        SearchHits<Book> bookSearchHits = esOps.search(query, Book.class);

        ElasticsearchAggregations aggregations = (ElasticsearchAggregations) bookSearchHits.getAggregations();
        Aggregate topCategories = aggregations.get("popularCategories").aggregation().getAggregate();

        List<StringTermsBucket> sTermsBuckets = topCategories.sterms().buckets().array();

        List<CategoryAvgRating> categoryList = sTermsBuckets.stream()
                .map(sTermBucket -> new CategoryAvgRating(
                        sTermBucket.key().stringValue(),
                        sTermBucket.docCount(),
                        sTermBucket.aggregations().get("averageRating").avg().value()))
                .toList();

        return new TopCategoriesWithAvgRating(categoryList);
    }

}
