# Analytics for File-Based Local Catalog Sync

## Overview
Add analytics to track file-based catalog sync performance: slow generation, stuck jobs, timeout frequency, and poll attempts across retries.

## Requirements Summary
1. Add `local_catalog_file` to `sync_strategy` property
2. Add `generation_duration_ms` and `poll_attempts` to `local_catalog_sync_completed` event
3. Add `last_generation_state` and `poll_attempts` to `local_catalog_sync_failed` event
4. ~~Add `local_catalog_beta_features_switch_toggled` event~~ **Already implemented**
5. Persist `poll_attempts` across worker retries (don't reset to 0)

---

## Implementation Steps (One Commit Each)

### Step 1: Extend SyncStrategy Enum

**Files:**
- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/home/items/products/WooPosProductsDataSource.kt`
- `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEvent.kt`

Add `LOCAL_CATALOG_FILE` to enum and update analytics mapping.

---

### Step 2: Extend PosLocalCatalogSyncResult Classes

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/localcatalog/WooPosSyncResult.kt`

Extend `Success` with `generationDurationMs` and `pollAttempts`.
Extend `CatalogGenerationTimeout` with `lastGenerationState` and `pollAttempts`.

---

### Step 3: Add Poll Attempts Persistence

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/datastore/WooPosPreferencesRepository.kt`

Add methods for site-specific poll attempts storage:
- `getFileBasedSyncPollAttempts(siteId)`
- `setFileBasedSyncPollAttempts(siteId, attempts)`
- `clearFileBasedSyncPollAttempts(siteId)`

---

### Step 4: Add Generation Duration Calculator

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/datastore/WooPosSyncTimestampManager.kt`

Add `calculateGenerationDuration(scheduledAt, completedAt)` method.

---

### Step 5: Update WooPosFileBasedSyncAction

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/localcatalog/WooPosFileBasedSyncAction.kt`

- Inject `WooPosPreferencesRepository` and `WooPosSyncTimestampManager`
- Track accumulated poll attempts across retries
- Track last generation state before timeout
- Calculate generation duration from `scheduledAt`/`completedAt`
- Extend `WooPosFileBasedSyncResult.Success` with new analytics data

---

### Step 6: Update Analytics Events

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/util/analytics/WooPosAnalyticsEvent.kt`

Update `LocalCatalogSyncCompleted` with `generationDurationMs` and `pollAttempts`.
Update `LocalCatalogSyncFailed` with `lastGenerationState` and `pollAttempts`.

---

### Step 7: Update Repository Analytics Tracking

**File:** `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/woopos/localcatalog/WooPosLocalCatalogSyncRepository.kt`

- Update `performFileBasedSync()` to pass new analytics data
- Update `trackSyncCompleted()` and `trackSyncFailed()` methods
- Clear poll attempts on successful sync completion

---

### Step 8: Add/Update Tests

- `WooPosFileBasedSyncActionTest.kt`
- `WooPosLocalCatalogSyncRepositoryTest.kt`
- `WooPosPreferencesRepositoryTest.kt`

---

## Data Flow: Poll Attempts Across Retries

```
Worker Run 1:
  └─ FileBasedSyncAction reads accumulated=0 from DataStore
  └─ Polls 20 times (total=20)
  └─ Timeout → stores accumulated=20 in DataStore
  └─ Returns CatalogGenerationTimeout(pollAttempts=20, lastState="in_progress")
  └─ Worker returns Result.retry()

Worker Run 2:
  └─ FileBasedSyncAction reads accumulated=20 from DataStore
  └─ Polls 5 times (total=25)
  └─ Success → clears DataStore
  └─ Returns Success(pollAttempts=25, generationDurationMs=45000)
  └─ Worker returns Result.success()
```

---

## Verification

1. Run unit tests: `./gradlew :WooCommerce:testWasabiDebugUnitTest`
2. Manual testing on emulator with logcat for tracking verification
3. Test staging site: https://site-for-woocommerce12a3fasdf45dfs6789.mystagingwebsite.com/

---

## Note: Beta Toggle Already Implemented

The `POS_LOCAL_CATALOG_BETA_FEATURES_SWITCH_TOGGLED` event is already implemented in:
- `AnalyticsEvent.kt` (line 557)
- `BetaFeaturesFragment.kt` (lines 82-96)

No additional work needed for this requirement.
