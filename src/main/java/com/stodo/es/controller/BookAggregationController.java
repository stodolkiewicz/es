package com.stodo.es.controller;

import com.stodo.es.service.aggregation.BookAggregationService;
import com.stodo.es.service.aggregation.TopCategories;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/books/aggregations")
public class BookAggregationController {

    private final BookAggregationService bookAggregationService;

    public BookAggregationController(BookAggregationService bookAggregationService) {
        this.bookAggregationService = bookAggregationService;
    }

    record AverageRatingResponse(Double averageRating) {}

    @GetMapping("/average-rating")
    public AverageRatingResponse averageRating() {
        return new AverageRatingResponse(bookAggregationService.averageRatingAggregation());
    }

    @GetMapping("/top-categories")
    public TopCategories topCategories() {
        return bookAggregationService.top10CategoriesAggregation();
    }
}
