# Semantic search (kNN) — ES DSL and Java

## What is this

- Full-text search matches words; semantic search matches **meaning**.
- Embedding model: text → vector. Similar meaning = vectors close to each other.
- Vector length depends on the model (`text-embedding-3-small` → 1536, `text-embedding-3-large` → 3072).
- Documents are embedded at index time, the user query at search time.
- Search = find documents with vectors nearest to the query vector = **kNN** (k nearest neighbors).

## Search flow

1. User sends query text — ES can't search text against vectors.
2. App calls the embedding model: query text → query vector (extra network hop + cost on every search).
3. App sends the query vector in a kNN request to ES.
4. ES returns documents with the nearest vectors.

## Prerequisites

- `dense_vector` field in the mapping (dims must match the embedding model — 1536 for `text-embedding-3-small`):

```json
"contentVector": {
  "type": "dense_vector",
  "dims": 1536,
  "similarity": "cosine",
  "index": true
}
```

- Every document has its vector stored (backfill done).
- The query text must be embedded with the **same model** that embedded the documents.

## HNSW

- Naive kNN: compare query vector with **every** stored vector. Exact, but O(n) — too slow at millions of docs.
- HNSW (Hierarchical Navigable Small World): layered graph of vectors, walked towards nearest neighbors in ~O(log n) hops. ES builds it for indexed `dense_vector` fields.
- Price: **approximate** results (`num_candidates` = accuracy dial), RAM for the graph, slower indexing.

## ES query language

```json
GET books/_search
{
  "knn": {
    "field": "contentVector",
    "query_vector": [0.021, -0.014, ...],
    "k": 10,
    "num_candidates": 50
  }
}
```

- `k` — how many results you want back.
- `num_candidates` — how many candidates HNSW inspects per shard before returning top-k.
  Higher = more accurate, slower. Rule of thumb: a few times larger than `k`.

Why "per shard": an ES index is split into shards (independent Lucene indexes,
possibly on different nodes). Each shard searches its own HNSW graph and returns
its local top-k; the coordinating node merges them into the final top-k.
- `knn` sits at the **top level** of the request, next to `query` — it is not a clause inside `query`.
- Score = `(1 + cosine_similarity) / 2`, so range is 0..1 and 1.0 means identical direction.

Kibana testing trick — reuse the vector of an existing document:

```json
GET books/_doc/9780002005883?_source_includes=contentVector
```

then paste it as `query_vector`. First hit should be the document itself with score ~1.0.

### Optional: pre-filtering

A `filter` inside `knn` restricts candidates **before** the vector search
(so you still get up to `k` results, all matching the filter):

```json
{
  "knn": {
    "field": "contentVector",
    "query_vector": [...],
    "k": 10,
    "num_candidates": 50,
    "filter": { "term": { "categories.keyword": "Fiction" } }
  }
}
```

## Java (Spring Data Elasticsearch, NativeQuery)

1. Embed the user query text (Spring AI `EmbeddingModel` returns `float[]`).
2. Convert `float[]` to `List<Float>` — the ES Java client's `KnnSearch` builder
   requires `List<Float>` (Java generics can't hold primitives; the client is
   code-generated from the API spec, so every JSON array becomes a `List<T>`).
3. Build `KnnSearch` and pass it via `withKnnSearches`.

```java
float[] vector = embeddingResponse.getResults().getFirst().getOutput();

List<Float> queryVector = new ArrayList<>(vector.length);
for (float f : vector) {
    queryVector.add(f);
}

KnnSearch knnSearch = KnnSearch.of(k -> k
        .field("contentVector")
        .queryVector(queryVector)
        .k(10)
        .numCandidates(50)
);

NativeQuery nativeQuery = NativeQuery.builder()
        .withKnnSearches(knnSearch)
        .build();

SearchHits<Book> hits = esOps.search(nativeQuery, Book.class);
```

Notes:

- `KnnSearch` builds the top-level `knn` section of the request.
- `withKnnSearches` accepts multiple `KnnSearch` objects — useful later for
  multi-query retrieval (N variants → N kNN searches).

- Response mapping: exclude `contentVector` from `_source` when you don't
  return it to the client

```java
NativeQuery nativeQuery = NativeQuery.builder()
        .withKnnSearches(knnSearch)
        .withSourceFilter(FetchSourceFilter.of(b -> b.withExcludes("contentVector")))
        .build();
```
