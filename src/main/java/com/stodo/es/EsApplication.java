package com.stodo.es;

import com.stodo.es.service.BookImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EsApplication {

	public static void main(String[] args) {
		SpringApplication.run(EsApplication.class, args);
	}

	@Bean
	CommandLineRunner importBooks(BookImportService importService) {
		return args -> importService.importBooks();
	}
}
