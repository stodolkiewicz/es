package com.stodo.es.service.search;

import java.util.List;

public record BookSearchResult(
        List<BookHit> hits,
        long totalHits
) {}