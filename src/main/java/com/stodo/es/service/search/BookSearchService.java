package com.stodo.es.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.stodo.es.document.Book;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookSearchService {

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
    public BookSearchResult searchByDescriptionMultiMatch(String description) {

        Query query = Query.of(qb -> qb.multiMatch(
                        MultiMatchQuery.of(mq -> mq
                                .query(description)
                                .fields("title^3", "description")
                                .type(TextQueryType.MostFields)
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

    private BookSearchResult mapSearchHitsToBookSearchResult(SearchHits<Book> bookSearchHits) {
        List<BookHit> bookHits = bookSearchHits.stream().map(
                book -> new BookHit(
                        book.getContent().getIsbn13(),
                        book.getContent().getTitle(),
                        book.getContent().getAuthors(),
                        book.getScore())
        ).toList();

        return new BookSearchResult(bookHits, bookSearchHits.getTotalHits());
    }
}
