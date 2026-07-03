package com.stodo.es.document;

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
@Document(indexName = "books")
@Mapping(mappingPath = "mappings/book-mappings.json")
public class Book {
    @Id
    private Integer id;

    private String isbn13;
    private String isbn10;

    private String title;
    private String subTitle;
    private List<String> authors;
    private List<String> categories;
    private String thumbnail;

    private String description;
    private short publishedYear;
    private Float averageRating;

}
