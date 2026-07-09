package com.stodo.es;

import com.stodo.es.embedding.BookVectorBackfillService;
import com.stodo.es.service.BookImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@SpringBootApplication
public class EsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsApplication.class, args);
	}

	@Bean
	@Order(1)
	CommandLineRunner importBooks(BookImportService importService) {
		return args -> importService.importBooks();
	}

	@Bean
	@Order(2)
	CommandLineRunner backfillBookVectors(BookVectorBackfillService backfillService) {
		return args -> backfillService.backfillBooks();
	}
}
