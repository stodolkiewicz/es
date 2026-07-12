package com.stodo.es.controller;

import com.stodo.es.service.search.BookSearchResult;
import com.stodo.es.service.search.fulltext.BookSearchService;
import com.stodo.es.service.search.semanticsearch.SemanticSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/books")
public class BookSearchController {

    private final BookSearchService bookSearchService;
    private final SemanticSearchService semanticSearchService;

    public BookSearchController(BookSearchService bookSearchService, SemanticSearchService semanticSearchService) {
        this.bookSearchService = bookSearchService;
        this.semanticSearchService = semanticSearchService;
    }

    record SearchByDescriptionRequest(String description) {}

    record MultiMatchSearchRequest(String query, Double averageRatingAbove) {}

    record BoolSearchRequest(String description, Double averageRatingAbove, String category, int preferYearAfter) {}

    record SemanticSearchRequest(String query, Double averageRatingAbove) {}

    @PostMapping("/search/match")
    public BookSearchResult searchBooks(@RequestBody SearchByDescriptionRequest request) {
        return bookSearchService.searchByDescription(request.description());
    }

    @PostMapping("/search/multi-match")
    public BookSearchResult searchBooksMultiMatch(@RequestBody MultiMatchSearchRequest request) {
        return bookSearchService.searchByDescriptionMultiMatch(request.query(), request.averageRatingAbove());
    }

    @PostMapping("/search/bool")
    public BookSearchResult searchBooksBool(@RequestBody BoolSearchRequest request) {
        return bookSearchService.sampleBoolQuery(
                request.description(),
                request.averageRatingAbove(),
                request.category(),
                request.preferYearAfter());
    }

    @PostMapping("/search/semantic")
    public BookSearchResult searchBooksSemantic(@RequestBody SemanticSearchRequest request) {
        return semanticSearchService.semanticSearch(request.query(), request.averageRatingAbove());
    }
}
