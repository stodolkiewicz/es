# Sample Queries (from exercises)

Solutions to the exercises, runnable in Kibana Dev Tools.

## Zadanie 1 — basics

Get a document by id (isbn13 is the `_id`):

```json
GET books/_doc/9780002005883
```

Full-text match on title:

```json
GET books/_search
{
  "query": {
    "match": { "title": "gilead" }
  }
}
```

Exact match on a category (`term` + `.keyword`):

```json
GET books/_search
{
  "query": {
    "term": { "categories.keyword": "American fiction" }
  }
}
```

Simple match on description (the one rewritten in Java in Zadanie 6):

```json
GET books/_search
{
  "query": {
    "match": { "description": "murder mystery" }
  }
}
```

## Zadanie 2a — bool: must + filter

Books matching "murder mystery" in description, only with rating >= 4.0
(rating must not affect the score).

```json
GET books/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "description": "murder mystery" } }
      ],
      "filter": [
        { "range": { "averageRating": { "gte": 4.0 } } }
      ]
    }
  }
}
```

## Zadanie 2b — bool: + must_not

Same, excluding the "Juvenile Fiction" category.
`term` on `.keyword` compares the whole list element — a `match` on the
analyzed field would also (wrongly) exclude books like
`["Juvenile literature", "American fiction"]` (array values are flattened).

```json
GET books/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "description": "murder mystery" } }
      ],
      "filter": [
        { "range": { "averageRating": { "gte": 4.0 } } }
      ],
      "must_not": [
        { "term": { "categories.keyword": "Juvenile Fiction" } }
      ]
    }
  }
}
```

## Zadanie 2c — bool: + should (boost, not filter)

Books published after 2000 get a score boost; older ones stay in results.
A `range` in `should` adds a constant +1 to matching documents.

```json
GET books/_search
{
  "query": {
    "bool": {
      "must": [
        { "match": { "description": "murder mystery" } }
      ],
      "filter": [
        { "range": { "averageRating": { "gte": 4.0 } } }
      ],
      "must_not": [
        { "term": { "categories.keyword": "Juvenile Fiction" } }
      ],
      "should": [
        { "range": { "publishedYear": { "gt": 2000 } } }
      ]
    }
  }
}
```

## Zadanie 3 — metric aggregation: avg

Average rating of all books; `size: 0` = no documents in the response.

```json
GET books/_search
{
  "size": 0,
  "aggs": {
    "avgAverageRating": {
      "avg": { "field": "averageRating" }
    }
  }
}
```

## Zadanie 4 — bucket aggregation: terms

Top 10 categories with book counts.

```json
GET books/_search
{
  "size": 0,
  "aggs": {
    "popular_categories": {
      "terms": {
        "field": "categories.keyword",
        "size": 10
      }
    }
  }
}
```

## Zadanie 5 — nested aggregation

Average rating inside each of the 5 most frequent categories,
buckets sorted by that average, only buckets with more than 10 books.

```json
GET books/_search
{
  "size": 0,
  "aggs": {
    "popular_categories": {
      "terms": {
        "field": "categories.keyword",
        "size": 5,
        "order": { "averageRating": "desc" },
        "min_doc_count": 11
      },
      "aggs": {
        "averageRating": {
          "avg": { "field": "averageRating" }
        }
      }
    }
  }
}
```

## Mapping / analysis — _analyze

Shows the tokens a field's analyzer produces — i.e. what actually sits
in the index. Compare `text` vs `keyword`:

```json
GET books/_analyze
{
  "field": "categories",
  "text": "Juvenile Fiction"
}
```

```json
GET books/_analyze
{
  "field": "categories.keyword",
  "text": "Juvenile Fiction"
}
```

- `categories` (text): tokens `["juvenile", "fiction"]` — split, lowercased
- `categories.keyword`: single token `"Juvenile Fiction"` — unchanged

## Zadanie 6 — same match query in Java (NativeQuery)

Equivalent of `match` on `description`, in a Spring service:

```java
NativeQuery query = NativeQuery.builder()
    .withQuery(q -> q.match(m -> m
        .field("description")
        .query("murder mystery")))
    .build();

SearchHits<Book> hits = elasticsearchOperations.search(query, Book.class);
```

Convention: `SearchHits` never leaves the service — map it to a result
record (DTO) with just the fields the consumer needs (+ `totalHits`).
