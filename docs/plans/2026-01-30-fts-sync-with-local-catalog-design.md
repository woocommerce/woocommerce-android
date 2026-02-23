# FTS Sync with Local Catalog Design

**Issue:** WOOMOB-2068
**Branch:** `woomob-2068-woo-posfts-sync-fts-index-with-local-catalog`
**Date:** 2026-01-30

## Overview

Keep FTS index in sync with local catalog. Update index on full sync and incremental sync. Remove deleted/trashed products from the index.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│  WooCommerce Module                                     │
│  ┌───────────────────────────────────────────────────┐  │
│  │  WooPosLocalCatalogSyncWithFts (NEW)              │  │
│  │  - Wraps WooPosLocalCatalogStore                  │  │
│  │  - Injects IsPosProductsFtsEnabled                │  │
│  │  - Coordinates FTS population                     │  │
│  └───────────────────────────────────────────────────┘  │
│                          │                              │
│                          ▼                              │
│  ┌───────────────────────────────────────────────────┐  │
│  │  WooPosLocalCatalogSyncRepository                 │  │
│  │  (uses SyncWithFts instead of Store directly)     │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│  FluxC Module                                           │
│  ┌───────────────────────────────────────────────────┐  │
│  │  WooPosLocalCatalogStore (existing)               │  │
│  │  WooPosSearchableFtsDao (existing)                │  │
│  │  WooPosProductsDao / WooPosVariationsDao          │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Key Decisions

1. **Wrapper class approach** - `WooPosLocalCatalogSyncWithFts` wraps store calls and adds FTS logic when flag is enabled. Easier to remove when releasing.

2. **Query parent product at FTS time** - No schema changes to `WooPosVariationEntity`. Look up parent product names during FTS population.

3. **Clear and repopulate for full sync** - Delete all FTS entries for site, then insert new ones. Simple and consistent with current approach.

4. **Delete + Insert for incremental sync** - FTS4 doesn't support upsert well. Delete existing entry then insert new one.

5. **Splash screen population** - On-demand FTS population for existing data happens during splash screen with loading state.

## FTS Population Logic

### Products → FTS Entity

```kotlin
WooPosSearchableFtsEntity(
    localSiteId = product.localSiteId.value.toString(),
    itemId = product.remoteId.value.toString(),
    parentProductId = "",  // empty for products
    name = product.name,
    sku = product.sku,
    barcode = product.globalUniqueId,
    attributeValues = ""   // empty for products
)
```

### Variations → FTS Entity

```kotlin
// 1. Group variations by parentProductId
// 2. Query parent product names in batch
// 3. Map each variation:

WooPosSearchableFtsEntity(
    localSiteId = variation.localSiteId.value.toString(),
    itemId = variation.remoteVariationId.value.toString(),
    parentProductId = variation.remoteProductId.value.toString(),
    name = parentProductName,  // from lookup
    sku = variation.sku,
    barcode = variation.globalUniqueId,
    attributeValues = extractAttributeValues(variation.attributesJson)
)
```

### Attribute Values Extraction

From JSON: `[{"id":12,"name":"Color","option":"Blue"},{"id":14,"name":"Size","option":"Medium"}]`
To string: `"Blue Medium"` (space-separated option values)

## Sync Flows

### 1. Full Sync (Splash Screen)

```
WooPosSplashViewModel
  → productsDataSource.prepopulateCache()
    → WooPosProductsInDbDataSource.prepopulateCache()
      → performInstantCatalogFullSync()
        → WooPosLocalCatalogSyncWithFts.storeCatalogData()
          → store.storeCatalogData() // existing
          → if (isFtsEnabled) populateFts() // new
```

### 2. On-Demand Population (Existing Data)

```
WooPosProductsInDbDataSource.prepopulateCache()
  → ensureFtsPopulated()
    → if (isFtsEnabled && productCount > 0 && isFtsTableEmpty)
      → populateFtsFromExistingData()
```

### 3. Incremental Sync

```
WooPosLocalCatalogSyncWithFts.upsertProducts()
  → store.upsertProducts()
  → if (isFtsEnabled) updateFtsForProducts()

WooPosLocalCatalogSyncWithFts.upsertVariations()
  → store.upsertVariations()
  → if (isFtsEnabled) updateFtsForVariations()

WooPosLocalCatalogSyncWithFts.deleteProducts()
  → store.deleteProducts()
  → if (isFtsEnabled) ftsDao.deleteProducts()

WooPosLocalCatalogSyncWithFts.deleteVariations()
  → store.deleteVariations()
  → if (isFtsEnabled) ftsDao.deleteVariations()
```

## Files to Create/Modify

### New Files

- `WooCommerce/.../localcatalog/WooPosLocalCatalogSyncWithFts.kt`

### Modify

- `WooPosSearchableFtsDao.kt` - add `countAllForSite(siteId)` method
- `WooPosLocalCatalogSyncRepository.kt` - use wrapper instead of store
- `WooPosProductsInDbDataSource.kt` - call `ensureFtsPopulated()` in `prepopulateCache()`

## Implementation Order

1. **Full sync flow from Splash** - Create wrapper, implement `storeCatalogData()` with FTS, add `ensureFtsPopulated()`
2. **Incremental sync** - Add FTS to `upsertProducts()`, `upsertVariations()`, `deleteProducts()`, `deleteVariations()`
3. **Tests** - Unit tests for wrapper class
