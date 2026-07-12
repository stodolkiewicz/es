package com.stodo.es.embedding;

import com.stodo.es.document.Book;
import com.stodo.es.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BookVectorBackfillService {
    private static final int BATCH_SIZE = 100;
    private final EmbeddingService embeddingService;
    private final BookRepository bookRepository;
    private final ElasticsearchOperations esOps;

    public BookVectorBackfillService(EmbeddingService embeddingService, BookRepository bookRepository, ElasticsearchOperations esOps) {
        this.embeddingService = embeddingService;
        this.bookRepository = bookRepository;
        this.esOps = esOps;
    }

    public void backfillBooks() {
        Page<Book> page = bookRepository.findBooksWithoutContentVector(PageRequest.of(0, BATCH_SIZE));
        if (page.isEmpty()) {
            log.info("All books already have embeddings, nothing to backfill");
            return;
        }

        long total = page.getTotalElements();
        long processed = 0;
        log.info("Backfilling embeddings for {} books", total);

        while (!page.isEmpty()) {
            List<Book> books = page.getContent();

            List<String> booksToBeEmbedded = books.stream()
                    .map(book ->
                            "book title: " + book.getTitle() + "\n" +
                                    "book subtitle: " + book.getSubtitle() + "\n" +
                                    "book description: " + book.getDescription()
                    )
                    .toList();

            EmbeddingResponse embeddings = embeddingService.getEmbeddings(booksToBeEmbedded);

            // embeddings come back in input order, so pair them with books by index
            List<UpdateQuery> updates = new ArrayList<>(books.size());
            for (int i = 0; i < books.size(); i++) {
                float[] vector = embeddings.getResults().get(i).getOutput();
                updates.add(
                    UpdateQuery.builder(books.get(i).getIsbn13())
                        .withDocument(
                            Document.create().append(
                                "contentVector",
                                vector
                            )
                        ).build()
                );
            }

            esOps.bulkUpdate(updates, Book.class);
            // make updated docs visible to the next findBooksWithoutContentVector call
            esOps.indexOps(Book.class).refresh();

            processed += books.size();
            log.info("Embedded batch of {} books ({}/{})", books.size(), processed, total);

            page = bookRepository.findBooksWithoutContentVector(PageRequest.of(0, BATCH_SIZE));
        }

        log.info("Backfill finished: {} books embedded", processed);
    }

}
