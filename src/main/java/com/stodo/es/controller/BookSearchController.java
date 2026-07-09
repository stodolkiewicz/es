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

    record SearchByDescriptionRequest(String description) {}

    record MultiMatchSearchRequest(String query) {}

    record BoolSearchRequest(String description, Double averageRatingAbove, String category, int preferYearAfter) {}

    @PostMapping("/search/match")
    public BookSearchResult searchBooks(@RequestBody SearchByDescriptionRequest request) {
        return bookSearchService.searchByDescription(request.description());
    }

    @PostMapping("/search/multi-match")
    public BookSearchResult searchBooksMultiMatch(@RequestBody MultiMatchSearchRequest request) {
        return bookSearchService.searchByDescriptionMultiMatch(request.query());
    }

    @PostMapping("/search/bool")
    public BookSearchResult searchBooksBool(@RequestBody BoolSearchRequest request) {
        return bookSearchService.sampleBoolQuery(
                request.description(),
                request.averageRatingAbove(),
                request.category(),
                request.preferYearAfter());
    }
}
