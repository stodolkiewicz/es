package com.stodo.es.service.search.hybrid;

import com.stodo.es.service.search.BookHit;
import com.stodo.es.service.search.BookSearchResult;
import com.stodo.es.service.search.fulltext.BookSearchService;
import com.stodo.es.service.search.semanticsearch.SemanticSearchService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class HybridSearchService {

    private static final double RRF_K = 60.0;

    private final SemanticSearchService semanticSearchService;
    private final BookSearchService bookSearchService;

    public HybridSearchService(SemanticSearchService semanticSearchService, BookSearchService bookSearchService) {
        this.semanticSearchService = semanticSearchService;
        this.bookSearchService = bookSearchService;
    }

    public BookSearchResult RFF(String userQuery, Double averageRatingAbove, int maxNumResultsFromEachSearch) {

        CompletableFuture<BookSearchResult> cfSematic = CompletableFuture.supplyAsync(
                () -> semanticSearchService.semanticSearch(
                        userQuery,
                        averageRatingAbove,
                        maxNumResultsFromEachSearch,
                        maxNumResultsFromEachSearch * 5
                )
        );

        CompletableFuture<BookSearchResult> cfFullText = CompletableFuture.supplyAsync(
                () -> bookSearchService.searchByDescriptionMultiMatch(
                        userQuery,
                        averageRatingAbove,
                        maxNumResultsFromEachSearch
                )
        );

        CompletableFuture.allOf(cfSematic, cfFullText).join();
        BookSearchResult fullTextResults = cfFullText.join();
        BookSearchResult semanticResults = cfSematic.join();

        return getRFFRankedResults(maxNumResultsFromEachSearch, semanticResults, fullTextResults);
    }

    // RRF_score(doc) = Σ 1 / (k + rank_i(doc))
    //  - sum over all result lists the document appears in
    //  - rank_i(doc) — the document's position in list i (1, 2, ...)
    //  - k — damping constant, standard value 60. Without it, the gap between rank 1 and rank 2 would dominate too much.
    private static BookSearchResult getRFFRankedResults(int numResults, BookSearchResult semanticResults, BookSearchResult fullTextResults) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, BookHit> bookByIsbn = new HashMap<>();

        for (BookSearchResult result : List.of(semanticResults, fullTextResults)) {

            List<BookHit> hits = result.hits();
            for (int i = 0; i < hits.size(); i++) {
                BookHit hit = hits.get(i);
                int rank = i + 1;

                double contribution = 1.0 / (RRF_K + rank);
                double previousScore = rrfScores.getOrDefault(hit.isbn13(), 0.0);
                rrfScores.put(hit.isbn13(), previousScore + contribution);

                bookByIsbn.putIfAbsent(hit.isbn13(), hit);
            }
        }

        List<BookHit> rankedHits = rrfScores.entrySet().stream()
                .sorted((o1, o2) -> o2.getValue().compareTo(o1.getValue()))
                .limit(numResults)
                .map(rffEntry -> {
                    BookHit original = bookByIsbn.get(rffEntry.getKey());

                    return new BookHit(
                            original.isbn13(),
                            original.title(),
                            original.subtitle(),
                            original.description(),
                            original.authors(),
                            original.categories(),
                            original.averageRating(),
                            rffEntry.getValue().floatValue()
                    );

                })
                .toList();

        return new BookSearchResult(rankedHits, rrfScores.size());
    }
}
