package com.stodo.es.service.search.semanticsearch;

import co.elastic.clients.elasticsearch._types.KnnSearch;
import com.stodo.es.document.Book;
import com.stodo.es.embedding.EmbeddingService;
import com.stodo.es.service.search.BookSearchResult;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.stodo.es.service.search.BookSearchResultMapper.mapSearchHitsToBookSearchResult;

@Service
public class SemanticSearchService {

    private static final int DEFAULT_NUM_RESULTS = 10;
    private static final int DEFAULT_NUM_CANDIDATES = 50;

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations esOps;

    public SemanticSearchService(EmbeddingService embeddingService, ElasticsearchOperations esOps) {
        this.embeddingService = embeddingService;
        this.esOps = esOps;
    }

    public BookSearchResult semanticSearch(String userQuery, Double averageRatingAbove, Integer numResults, Integer numCandidates) {
        EmbeddingResponse embeddingResponse = embeddingService.getEmbeddings(List.of(userQuery));
        float[] vector = embeddingResponse.getResults().getFirst().getOutput();

        // boxing of float[]
        List<Float> queryVector = new ArrayList<>(vector.length);
        for (float f : vector) {
            queryVector.add(f);
        }

        int effectiveNumResults = numResults != null ? numResults : DEFAULT_NUM_RESULTS;
        int effectiveNumCandidates = numCandidates != null ? numCandidates : DEFAULT_NUM_CANDIDATES;

        KnnSearch.Builder knnSearchBuilder = new KnnSearch.Builder()
                .field("contentVector")
                .k(effectiveNumResults)
                .numCandidates(effectiveNumCandidates)
                .queryVector(queryVector);

        if (averageRatingAbove != null) {
            knnSearchBuilder.filter(f -> f.range(rq -> rq.number(nrq -> nrq
                    .field("averageRating")
                    .gte(averageRatingAbove))));
        }


        NativeQuery nativeQuery = NativeQuery.builder()
                .withKnnSearches(knnSearchBuilder.build())
                .withSourceFilter(FetchSourceFilter.of(b -> b.withExcludes("contentVector")))
                .withMaxResults(effectiveNumResults)
                .build();

        SearchHits<Book> bookSearchHits = esOps.search(nativeQuery, Book.class);

        return mapSearchHitsToBookSearchResult(bookSearchHits);
    }
}
