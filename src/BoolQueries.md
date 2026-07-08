# Bool Queries

The `bool` query combines multiple queries into one. Single queries like `match` or `range` can each express only one condition — `bool` is the mechanism that lets you say "full-text match on description AND rating >= 4.0 AND NOT this category" in a single request.

## Key concept: matching vs scoring

These are **two separate things**:

1. **Matching is binary** — every query first answers: does this document match, yes or no?
   For `match`, "matches" means the field contains **at least one** term from the query
   (unless changed with `operator: and` or `minimum_should_match`).
2. **Score is computed only for matching documents** — there is no score threshold.
   Score is used purely to **sort** the matching documents (relevance ranking).

## The four clauses

| Clause     | Must match? | Affects score? | Typical use                          |
|------------|-------------|----------------|--------------------------------------|
| `must`     | yes         | yes            | full-text search (`match`)           |
| `filter`   | yes         | no             | exact values, ranges (`term`, `range`) |
| `should`   | no*         | yes (boost)    | "nice to have" — boosts relevance    |
| `must_not` | must NOT    | no             | excluding documents                  |

\* If there is no `must` and no `filter`, at least one `should` clause must match
(controllable with `minimum_should_match`).

### `must`
Document must match the clause **and** the clause contributes to the score.

### `filter`
Document must match the clause, but the score is **not** computed for it.
Rejects documents exactly like `must` — the only difference is scoring.
Because there is no scoring, filter clauses are **cached** and faster.
Rule of thumb: yes/no conditions (exact terms, ranges, dates) go into `filter`.

### `should`
Matching documents get a **higher score**, but non-matching ones are **not rejected**
(as long as `must`/`filter` is present). Use it to boost, not to filter.

### `must_not`
The opposite of `filter`: if the document matches, it is **thrown out**.
No scoring, cached, just exclusion.

## Example (books index)

Find books about a murder mystery, only with rating >= 4.0,
excluding Juvenile Fiction, preferring books published after 2000:

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

- rating and category do not change the ranking — they only include/exclude
- `description` match and the `should` clause decide the order of results
- a book from 1995 still appears in results, just scored lower than a 2005 one
  (all else being equal)

## filter vs must_not — quick comparison

```json
"filter":   [{ "term": { "categories.keyword": "Fiction" } }]  // only Fiction remains
"must_not": [{ "term": { "categories.keyword": "Fiction" } }]  // everything EXCEPT Fiction
```

Both skip scoring and are cached; they are mirror opposites of each other.
