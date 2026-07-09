package com.stodo.es.service.aggregation;

import java.util.List;

public record TopCategoriesWithAvgRating(List<CategoryAvgRating> categories) {

    public record CategoryAvgRating(String category, long bookCount, double avgRating) {}
}
