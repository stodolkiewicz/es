# YouTube Playlist Plan — Elasticsearch + Spring Boot

Each episode: standalone entry point from search, 10–20 min,
starts from the repo (`docker compose up`). Titles target search phrases.

## Episodes

| # | Topic | Target phrase | Length | Material status |
|---|-------|---------------|--------|-----------------|
| 1 | Setup: docker-compose (ES + Kibana), Kaggle books.csv, auto-import on Spring Boot startup | "elasticsearch spring boot setup" | 5–8 min | code done |
| 2 | Mapping & text analysis: `text` vs `keyword`, analyzers, `_analyze` API | "elasticsearch text vs keyword" | 10–15 min | in progress (learning) |
| 3 | Basic queries: `match`, `multi_match`, `term`, `range` | "elasticsearch query dsl tutorial" | 10–15 min | `Queries.md` done |
| 4 | Bool query: `must` / `filter` / `should` / `must_not`, matching vs scoring | "elasticsearch bool query explained" | 10–15 min | `BoolQueries.md` done |
| 5 | Aggregations: metric vs bucket, `terms`, nesting, `order`, `min_doc_count`, `range`/`histogram` | "elasticsearch aggregations tutorial" | 10–15 min | `Aggs.md` done |
| 6 | Same in Java: Spring Data Elasticsearch + `NativeQuery` — rewrite queries from ep. 3–5 | "spring data elasticsearch native query" | 15–25 min | not started |
| 7 | Vector search: `dense_vector` mapping, embeddings (Spring AI), `knn` query, HNSW params | "spring ai elasticsearch vector search" | 15–25 min | Dawid knows Spring AI side; ES syntax to learn |
| 8 | Hybrid search: BM25 + kNN, score scales problem, RRF; free alternative: `_msearch` + client-side RRF | "hybrid search elasticsearch rrf spring boot" | 15–25 min | theory discussed |
| 9 | Smart search (multi-query retrieval): LLM expands user query into 3–5 variants → embed batch → N× kNN → single RRF over all lists (semantic + full-text, optional per-list weights) → one ranking. ChatClient + structured output, hand-rolled `RrfRanker` (unit-testable) | "query expansion rag spring ai elasticsearch" | 15–25 min | idea, design agreed |

## Workflow (learn-by-teaching)

1. Cover the topic in learning sessions (exercises + review)
2. Write the script in own words — comprehension gaps show up here
3. Bring gaps back to the session before recording
4. Record

`.md` files in `src/` double as script notes — keep them tutorial-ready.

## Production notes

- ~4 h per 10-min episode (Dawid's own calibration) → whole playlist ≈ 30–35 h
- consistency beats frequency: stable 2/week > 5 in week one, then silence
- first 30 seconds: show the end result, no long intros
- cut everything that is "watching code being typed" — pre-written snippets or time-lapse
- episodes 7–8 have the strongest search potential (RAG/AI wave) — they can
  also work as a standalone "RAG-ready search" mini-series pulling viewers
  back into the playlist
