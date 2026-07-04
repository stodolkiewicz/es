# Queries

#### Get all books
```
GET books/_search
{
    "query": {
        "match_all": {}
    }
}
```

#### Match
```
GET books/_search
{
    "query": {
        "match": {
            "description": {
                "query": "mysterious disappearance of",
                "operator": "or", // default
                "fuzziness": "AUTO",
                "minimum_should_match": 2
            }
        }
    }
}
```

#### Multi-match
```json
"query": {
    "multi_match": {
        "query": "medieval europe lives village",
        "fields": ["description", "title^3"],
        "type": "most_fields"
    }
}
```

#### Term
```json
"query": {
    "term": {
      "categories.keyword": "American fiction"
    }
}
```

#### Range
```json
"query": {
    "range": {
        "averageRating": {
            "gte": 4.7,
            "lte": 4.8
        }
    }
}
```