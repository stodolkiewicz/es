# Aggregations

Aggregations compute statistics over documents matching the query.
Two main kinds:

- **metric** — one value computed from a set of documents (`avg`, `min`, `max`, `sum`, `cardinality`)
- **bucket** — split documents into groups (`terms`, `range`, `histogram`); each bucket can have its own sub-aggregations

`"size": 0` at the top level skips returning documents — response contains only aggregation results.

## Metric: average rating of all books

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

## Bucket: top categories with doc counts

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

- `size` (inside `terms`) = number of buckets, default 10; not to be confused
  with the top-level `size` which controls documents
- there is no "all buckets" option — use a large `size`
  (hard response limit: `search.max_buckets` = 65536),
  or the `composite` aggregation to page through buckets
- `cardinality` aggregation returns the number of unique values
  (approximate for high-cardinality fields) — useful to check what `size` is enough
- aggregate on the `.keyword` sub-field: buckets are whole values
  ("Juvenile Fiction"), not analyzed tokens ("juvenile", "fiction")

## Nested: average rating per category

Sub-aggregation is computed **separately inside each parent bucket**.

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

### Sorting buckets — `order`

- default: `{ "_count": "desc" }` (biggest buckets first)
- `{ "_key": "asc" }` — alphabetically by bucket value
- `{ "<sub-agg name>": "desc" }` — by a sub-aggregation result (as above)
- top-level `sort` sorts **documents**, not buckets — with `size: 0` it does nothing

Pitfall: ordering by a sub-aggregation on a multi-shard index can be
**inaccurate** — each shard picks its top buckets before merging.

## Bucket: `range` — hand-defined intervals

```json
GET books/_search
{
  "size": 0,
  "aggs": {
    "rating_ranges": {
      "range": {
        "field": "averageRating",
        "ranges": [
          { "to": 3.0 },
          { "from": 3.0, "to": 4.0 },
          { "from": 4.0 }
        ]
      }
    }
  }
}
```

- `from` is **inclusive**, `to` is **exclusive**
- ranges may overlap — a document then lands in multiple buckets
- optional `"key"` per range gives the bucket a custom name

Related: `histogram` generates fixed-width buckets automatically
(`"interval": 10` on `publishedYear` = decades); `date_histogram` is the
date variant (`"calendar_interval": "year"`). All of them accept
sub-aggregations like any other bucket agg.

### Filtering buckets — `min_doc_count`

- `min_doc_count: 11` — keep only buckets with more than 10 documents
- default is 1; `0` also shows empty buckets (categories the query filtered out entirely)
- filtering by a **sub-aggregation** result (e.g. avg > 4.0) needs the
  `bucket_selector` pipeline aggregation
