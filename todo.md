TODO: Spring Boot + Elasticsearch — hybrid search na książkach

Zakres: wyszukiwarka książek (dataset 7k Books) z pełnym tekstem + wyszukiwaniem semantycznym (OpenAI embeddings) połączonym przez RRF. Outbox/Kafka celowo pominięte na razie — osobna faza na później.

Faza 0 — Setup środowiska


docker-compose.yml z Elasticsearch (single-node, lokalnie)
Zdecydować: security włączone czy wyłączone lokalnie (prościej: wyłączone na start)
docker-compose up, sprawdzić curl localhost:9200 — klaster odpowiada
Projekt Spring Boot ze start.spring.io: spring-boot-starter-data-elasticsearch, spring-boot-starter-web
Skonfigurować spring.elasticsearch.uris w application.yml
Prosty health-check endpoint / test że ElasticsearchClient się wstrzykuje i łączy


Faza 1 — Dataset i model danych


Pobrać dataset 7k Books with metadata (Kaggle: dylanjcastillo/7k-books-with-metadata)
Obejrzeć CSV — sprawdzić realne wartości pól, braki danych, encoding
Zdefiniować klasę Book (@Document) z polami: title, subtitle, authors, categories, description, publishedYear, averageRating, numPages, ratingsCount
Zaprojektować mapping indeksu ręcznie (nie auto-generowany):

title, description, authors → text
categories → keyword (do filtrowania) + ewentualnie kopia jako text
publishedYear, numPages, ratingsCount → integer
averageRating → float
descriptionEmbedding → dense_vector, wymiar 1536 (text-embedding-3-small), similarity: cosine



Utworzyć indeks w Elasticsearch z tym mappingiem (przez klienta albo curl/Dev Tools)


Faza 2 — Loader CSV → Elasticsearch


Dodać parser CSV (np. OpenCSV albo Jackson CSV)
Wczytać plik, zmapować wiersze na obiekty Book
Obsłużyć braki danych (puste description, brakujące categories itd. — nie wywalać całego importu na jednym złym wierszu)
Zaimplementować _bulk insert (batche np. po 200-500 dokumentów, nie pojedyncze żądania)
Endpoint albo CommandLineRunner uruchamiający import
Zweryfikować w Elasticsearch: _count zgadza się z liczbą wierszy w CSV


Faza 3 — Podstawowe wyszukiwanie pełnotekstowe


Endpoint GET /books/search?q=...
Zapytanie match/multi_match po title + description + authors
Przetestować kilka zapytań, sprawdzić czy ranking BM25 ma sens
(opcjonalnie) filtrowanie po categories jako dodatkowy parametr


Faza 4 — Integracja z OpenAI Embeddings


Konto OpenAI + klucz API, zmienna środowiskowa (nigdy w kodzie/repo)
Klient HTTP do OpenAI (RestClient/WebClient albo oficjalny SDK)
Serwis: tekst → embedding (text-embedding-3-small)
Zdecydować kiedy liczyć embeddingi: w loaderze, przed insertem do ES (jednorazowo dla całego datasetu)
Batchować wywołania do OpenAI (nie jeden request na książkę — API przyjmuje tablicę tekstów)
Rozszerzyć loader: dla każdej książki policzyć embedding z description, zapisać w descriptionEmbedding
Zweryfikować w Elasticsearch, że pole ma faktycznie 1536 liczb, nie null


Faza 5 — Semantyczne i hybrydowe wyszukiwanie


Endpoint GET /books/semantic-search?q=... — embedujesz zapytanie przez OpenAI, robisz czyste knn
Porównać wyniki: to samo zapytanie przez match vs przez knn — zobaczyć różnicę
Zbudować retriever z rrf łączący standard (match) + knn
Endpoint GET /books/hybrid-search?q=... zwracający wynik po RRF
Przygotować 3-4 przykładowe zapytania, które ładnie pokazują przewagę hybrid nad samym BM25 (np. opisowe, nie dosłowne zapytania — "książka o kimś kto traci pamięć")