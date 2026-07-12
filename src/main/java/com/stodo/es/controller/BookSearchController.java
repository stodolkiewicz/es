package com.stodo.es.controller;

import com.stodo.es.service.search.BookSearchResult;
import com.stodo.es.service.search.fulltext.BookSearchService;
import com.stodo.es.service.search.hybrid.HybridSearchService;
import com.stodo.es.service.search.semanticsearch.SemanticSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/books")
public class BookSearchController {

    private final BookSearchService bookSearchService;
    private final SemanticSearchService semanticSearchService;
    private final HybridSearchService hybridSearchService;

    public BookSearchController(BookSearchService bookSearchService, SemanticSearchService semanticSearchService, HybridSearchService hybridSearchService) {
        this.bookSearchService = bookSearchService;
        this.semanticSearchService = semanticSearchService;
        this.hybridSearchService = hybridSearchService;
    }

    record SearchByDescriptionRequest(String description) {}

    @PostMapping("/search/match")
    public BookSearchResult searchBooks(@RequestBody SearchByDescriptionRequest request) {
        return bookSearchService.searchByDescription(request.description());
    }


    record MultiMatchSearchRequest(String query, Double averageRatingAbove, Integer numResults) {}

    @PostMapping("/search/multi-match")
    public BookSearchResult searchBooksMultiMatch(@RequestBody MultiMatchSearchRequest request) {
        return bookSearchService.searchByDescriptionMultiMatch(request.query(), request.averageRatingAbove(), request.numResults());
    }

    record BoolSearchRequest(String description, Double averageRatingAbove, String category, int preferYearAfter) {}

    @PostMapping("/search/bool")
    public BookSearchResult searchBooksBool(@RequestBody BoolSearchRequest request) {
        return bookSearchService.sampleBoolQuery(
                request.description(),
                request.averageRatingAbove(),
                request.category(),
                request.preferYearAfter());
    }

    record SemanticSearchRequest(String query, Double averageRatingAbove, Integer numResults, Integer numCandidates) {}

    @PostMapping("/search/semantic")
    public BookSearchResult searchBooksSemantic(@RequestBody SemanticSearchRequest request) {
        return semanticSearchService.semanticSearch(
                request.query(),
                request.averageRatingAbove(),
                request.numResults(),
                request.numCandidates()
        );
    }

    record HybridSearchRequest(String query, Double averageRatingAbove, int maxNumResultsFromEachSearch) {}

    @PostMapping("/search/hybrid")
    public BookSearchResult searchBooksHybrid(@RequestBody HybridSearchRequest request) {
        return hybridSearchService.RFF(
                request.query(),
                request.averageRatingAbove(),
                request.maxNumResultsFromEachSearch()
        );
    }
}
