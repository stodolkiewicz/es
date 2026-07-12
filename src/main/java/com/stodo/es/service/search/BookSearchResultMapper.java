package com.stodo.es.service.search;

import com.stodo.es.document.Book;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

public final class BookSearchResultMapper {

    private BookSearchResultMapper() {}

    public static BookSearchResult mapSearchHitsToBookSearchResult(SearchHits<Book> bookSearchHits) {
        List<BookHit> bookHits = bookSearchHits.stream().map(
                book -> new BookHit(
                        book.getContent().getIsbn13(),
                        book.getContent().getTitle(),
                        book.getContent().getAuthors(),
                        book.getContent().getCategories(),
                        book.getContent().getAverageRating(),
                        book.getScore())
        ).toList();

        return new BookSearchResult(bookHits, bookSearchHits.getTotalHits());
    }
}
