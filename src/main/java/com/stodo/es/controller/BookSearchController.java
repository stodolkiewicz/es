package com.stodo.es.controller;

import com.stodo.es.service.search.BookSearchResult;
import com.stodo.es.service.search.BookSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/books")
public class BookSearchController {

    private final BookSearchService bookSearchService;

    public BookSearchController(BookSearchService bookSearchService) {
        this.bookSearchService = bookSearchService;
    }

    record SearchBooksRequestBody(String description){};

    @PostMapping("/search")
    public BookSearchResult searchBooks(@RequestBody SearchBooksRequestBody searchBooksRequestBody) {
        BookSearchResult bookSearchResult = bookSearchService.searchByDescription(searchBooksRequestBody.description());

        return bookSearchResult;
    }
}
