package com.stodo.es.service.search.fulltext;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.stodo.es.document.Book;
import com.stodo.es.service.search.BookSearchResult;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import static com.stodo.es.service.search.BookSearchResultMapper.mapSearchHitsToBookSearchResult;

@Service
public class BookSearchService {

    private static final int DEFAULT_NUM_RESULTS = 10;

    private final ElasticsearchOperations esOps;

    public BookSearchService(ElasticsearchOperations esOps) {
        this.esOps = esOps;
    }

    /*
    GET books/_search
    {
        "query": {
            "match": { "description": "fiction novel good" }
        }
    }
    */
    public BookSearchResult searchByDescription(String description) {

        Query query = Query.of(qb -> qb.match(
                MatchQuery.of(mq -> mq
                        .field("description")
                        .query(description)
                )
            )
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .build();

        SearchHits<Book> bookSearchHits
                = esOps.search(nativeQuery, Book.class);

        return mapSearchHitsToBookSearchResult(bookSearchHits);
    }


    /*
    GET books/_search
    {
        "query": {
            "multi_match": {
                "query": "murder mystery",
                "fields": ["title^3", "description"],
                "type": "most_fields"
            }
        }
    }
    */
    public BookSearchResult searchByDescriptionMultiMatch(String description, Double averageRatingAbove, Integer numResults) {

        Query multiMatchQuery = Query.of(qb -> qb.multiMatch(
                        MultiMatchQuery.of(mq -> mq
                                .query(description)
                                .fields("title", "subtitle", "description")
                                .type(TextQueryType.MostFields)
                        )
                )
        );

        BoolQuery.Builder boolQuery = new BoolQuery.Builder().must(multiMatchQuery);

        if(averageRatingAbove != null) {
            boolQuery.filter(fq -> fq.range(rq -> rq.number(
                    nq -> nq
                            .field("averageRating")
                            .gte(averageRatingAbove)
            )));
        }

        Query query = Query.of(q -> q.bool(boolQuery.build()));

        int effectiveNumResults = numResults != null ? numResults : DEFAULT_NUM_RESULTS;

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withMaxResults(effectiveNumResults)
                .build();

        SearchHits<Book> bookSearchHits
                = esOps.search(nativeQuery, Book.class);

        return mapSearchHitsToBookSearchResult(bookSearchHits);
    }

    /*
    Books published after 2000 get a score boost; older ones stay in results. A range in should adds a constant +1 to matching documents.

    GET books/_search
    {
      "query": {
        "bool": {
          "must": [
            { "match": { "description": "murder mystery" } }
          ],
          "filter": [
            { "range": { "averageRating": { "gte": 4.0 } } }
          ],
          "must_not": [
            { "term": { "categories.keyword": "Juvenile Fiction" } }
          ],
          "should": [
            { "range": { "publishedYear": { "gt": 2000 } } }
          ]
        }
      }
    }
    */
    public BookSearchResult sampleBoolQuery(String description, Double averageRatingAbove, String category, int preferYearAfter) {

        BoolQuery bq = new BoolQuery.Builder()
                .must(m -> m.match(
                        mq -> mq
                                .field("description")
                                .query(description)
                ))
                .filter(f -> f.range(
                        rq -> rq.number(
                                nrq -> nrq
                                        .field("averageRating")
                                        .gte(averageRatingAbove)
                                )
                ))
                .mustNot(mn -> mn.term(
                        tq -> tq
                                .field("categories.keyword")
                                .value(category)
                                .caseInsensitive(true)
                        )
                )
                .should(sq -> sq.range(
                        rq -> rq.number(
                                nq -> nq
                                        .field("publishedYear")
                                        .gt((double) preferYearAfter)
                        )
                ))
                .build();

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(boolQuery -> boolQuery.bool(bq)))
                .build();

        return mapSearchHitsToBookSearchResult(esOps.search(query, Book.class));
    }
}
