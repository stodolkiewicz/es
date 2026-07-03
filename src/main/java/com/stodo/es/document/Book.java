package com.stodo.es.document;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@Document(indexName = "books")
@Mapping(mappingPath = "mappings/book-mappings.json")
public class Book {
    @Id
    private String id;

    private String isbn13;
    private String isbn10;

    private String title;
    private String subtitle;
    private List<String> authors;
    private List<String> categories;
    private String thumbnail;

    private String description;
    private Short publishedYear;
    private Float averageRating;

}
