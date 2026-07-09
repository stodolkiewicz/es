package com.stodo.es.repository;

import com.stodo.es.document.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends CrudRepository<Book, String> {
    @Query("""
          { "bool": { "must_not": [ { "exists": { "field": "contentVector" } } ] } }
          """)
    Page<Book> findBooksWithoutContentVector(Pageable pageable);

}
