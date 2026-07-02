package com.stodo.es.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;

@Repository
public class ESRepository {
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    public void a() {
    }
}
