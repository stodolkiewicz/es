package com.stodo.es.service.search;

import java.util.List;

public record BookHit(String isbn13, String title, List<String> authors, float score) {}