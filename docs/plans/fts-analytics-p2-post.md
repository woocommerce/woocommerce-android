# FTS Search: Analytics

We are adding Full-Text Search (FTS) to POS product search. To understand how it performs in production and whether the ranking works well, we need analytics. Here is what I propose to track.

## What we want to learn

- **Is FTS search fast enough?** Especially on large catalogs.
- **Does ranking work?** Do users find the right product quickly or scroll deep into results?
- **How much overhead does FTS indexing add to sync?**

## Proposed Events

### 1. New event: `fts_search_performed`

Fires when a search query returns results (or no results) from the FTS index.

| Property | Type | Description |
|---|---|---|
| `search_duration_ms` | Long | Time to execute the FTS query and hydrate results. |
| `results_count` | Int | Number of results returned for this page. |
| `total_catalog_size` | Int | Total number of items in the FTS index. |
| `query_term_count` | Int | Number of search tokens (e.g. "blue shirt" = 2). |
| `has_results` | Boolean | Whether the search returned any results. |

**Why:** Lets us see latency percentiles vs catalog size. Zero-result rate tells us if tokenizer and indexed fields cover real search patterns.

### 2. New event: `fts_index_built`

Fires when the FTS index is rebuilt during sync (full or incremental).

| Property | Type | Description |
|---|---|---|
| `sync_type` | String | `full` or `incremental`. |
| `index_duration_ms` | Long | Time spent on FTS indexing only (not network). |
| `products_indexed` | Int | Number of products written to FTS table. |
| `variations_indexed` | Int | Number of variations written to FTS table. |
| `total_entities` | Int | Total rows in FTS table after indexing. |

**Why:** We already track total sync duration in `local_catalog_sync_completed`. This event separates the FTS indexing portion so we can see: "sync took 5s total, 800ms was FTS indexing."

### 3. New event: `fts_search_result_tapped`

Fires when a user taps a search result that came from FTS.

| Property | Type | Description |
|---|---|---|
| `result_position` | Int | Position of the tapped item in results (0-based). |
| `result_type` | String | `product` or `variation`. |
| `query_term_count` | Int | Number of search tokens in the query. |

**Why:** This is a proxy for search quality (Mean Reciprocal Rank). If users consistently tap position 0-1, ranking works. If they scroll to position 8+, it does not.

## What I decided not to track

- **Query text** - privacy concern; term count is enough.
- **Per-keystroke latency** - too noisy; `fts_search_performed` with duration already covers this.
- **FTS vs LIKE comparison** - the feature flag controls this; once FTS is default, LIKE data becomes useless.
- **Pagination events** - low signal; first page result count tells the story.

## Success indicators

We consider FTS search successful if:

- **Search is fast:** p95 search latency stays under 100ms, even on larger catalogs.
- **Ranking is useful:** most taps happen in the top 3 results.
- **Zero-result rate is low:** less than 15% of searches return nothing.
- **Indexing overhead is small:** FTS indexing adds less than 20% to total sync duration.

## Relationship with existing events

- `local_catalog_sync_completed` already tracks `sync_duration_ms`, `products_synced`, `variations_synced`. The new `fts_index_built` gives the FTS-specific breakdown within that sync.
- `search_remote_results_fetched` tracks remote/API search. The new `fts_search_performed` is the local FTS equivalent.

Thanks for reading! Let me know if you think we are missing something or if any of these events are not useful.

#woo-pos #pos-local-catalog
