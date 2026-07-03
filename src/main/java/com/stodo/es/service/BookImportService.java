package com.stodo.es.service;

import com.stodo.es.document.Book;
import com.stodo.es.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookImportService {

    private static final int BATCH_SIZE = 1000;

    private final BookRepository bookRepository;

    @Value("${app.books-csv-path:books.csv}")
    private Path csvPath;

    public long importBooks() {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        long imported = 0;
        List<Book> batch = new ArrayList<>(BATCH_SIZE);

        try (Reader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            for (CSVRecord row : parser) {
                batch.add(toBook(row));
                if (batch.size() == BATCH_SIZE) {
                    imported += flush(batch);
                }
            }
            imported += flush(batch);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + csvPath, e);
        }

        log.info("Import finished: {} books", imported);
        return imported;
    }

    private long flush(List<Book> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        bookRepository.saveAll(batch);
        int size = batch.size();
        log.info("Indexed batch of {} books", size);
        batch.clear();
        return size;
    }

    private Book toBook(CSVRecord row) {
        String isbn13 = emptyToNull(row.get("isbn13"));
        return Book.builder()
                // isbn13 as _id keeps the import idempotent: re-running
                // overwrites documents instead of duplicating them
                .id(isbn13)
                .isbn13(isbn13)
                .isbn10(emptyToNull(row.get("isbn10")))
                .title(emptyToNull(row.get("title")))
                .subtitle(emptyToNull(row.get("subtitle")))
                .authors(splitList(row.get("authors")))
                .categories(splitList(row.get("categories")))
                .thumbnail(emptyToNull(row.get("thumbnail")))
                .description(emptyToNull(row.get("description")))
                .publishedYear(parseShort(row.get("published_year")))
                .averageRating(parseFloat(row.get("average_rating")))
                .build();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static Short parseShort(String value) {
        String v = emptyToNull(value);
        return v == null ? null : Short.parseShort(v);
    }

    private static Float parseFloat(String value) {
        String v = emptyToNull(value);
        return v == null ? null : Float.parseFloat(v);
    }
}
