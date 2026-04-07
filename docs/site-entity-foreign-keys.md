# SiteEntity Foreign Key Constraints

## Background

Prior to the Room migration, some WC database tables lived in our legacy WellSQL database (`wp-fluxc`) alongside core tables like `SiteModel`. WooCommerce-specific tables were tagged with `@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)` to control lazy initialization, but they still resided in the same SQLite database file. This allowed foreign key constraints such as:

```
FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE
```

## The Problem

During the Room migration, tables were split into two separate database files:

| Database | File | Module | Contents |
|----------|------|--------|----------|
| `wp-android-database` | `WPAndroidDatabase` | `libs/fluxc` | Core tables (SiteEntity, AccountEntity, etc.) |
| `wc-android-database` | `WCAndroidDatabase` | `libs/fluxc-plugin` | WooCommerce tables (OrderEntity, ProductEntity, etc.) |

SQLite foreign key constraints only work **within a single database file**. Since `SiteEntity` lives in `wp-android-database` and all WC entities live in `wc-android-database`, FK constraints referencing `SiteEntity` from WC entities are **impossible** with the current architecture.

## Impact

Five WC entities previously had FK constraints to `SiteModel` in WellSQL. These constraints were lost when the entities were migrated to Room:

| Entity | Table | FK Column | WellSQL FK | Room FK |
|--------|-------|-----------|------------|---------|
| `WCProductTagModel` | `ProductTagEntity` | `localSiteId` | Yes | No (cross-database) |
| `WCProductShippingClassModel` | `ProductShippingClassEntity` | `localSiteId` | Yes | No (cross-database) |
| `WCOrderSummaryModel` | `OrderSummaryEntity` | `siteId` | Yes | No (cross-database) |
| `WCProductCategoryModel` | `ProductCategoryEntity` | `localSiteId` | Yes | No (cross-database) |
| `WCProductReviewModel` | `ProductReviewEntity` | `localSiteId` | Yes | No (cross-database) |

The following cascade delete tests were also removed since they can never pass with the current two-database architecture:

| Test File | Test Method |
|-----------|-------------|
| `ProductTagsDaoTest.kt` | `testDeleteSiteDeletesProductTags()` |
| `OrderSummaryDaoTest.kt` | `testDeleteSiteDeletesAllOrderSummaries()` |
| `ProductShippingClassesDaoTest.kt` | `testDeleteSiteDeletesProductShippingClassList()` |
| `ProductReviewsDaoTest.kt` | `testDeleteSiteDeletesAllProductReviews()` |

Additionally, 27 other WC entities reference `localSiteId` but never had FK constraints in WellSQL either.

## WC Database (`wc-android-database`) — All Entities with Site ID

| Entity | Column | Type | FK to SiteEntity | Had WellSQL FK |
|--------|--------|------|------------------|----------------|
| `BookingEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `BookingResourceEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `CouponEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `CouponEmailEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `CustomerFromAnalyticsEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `GatewayEntity` | `siteId` | `LocalId` | Not possible | No |
| `GlobalAddonGroupEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `InboxNoteEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `MetaDataEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `OrderEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `OrderNoteEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `OrderSummaryEntity` | `siteId` | `LocalId` | Not possible | Yes |
| `ProductCategoryEntity` | `localSiteId` | `LocalId` | Not possible | Yes |
| `ProductEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `ProductReviewEntity` | `localSiteId` | `LocalId` | Not possible | Yes |
| `ProductShippingClassEntity` | `localSiteId` | `LocalId` | Not possible | Yes |
| `ProductTagEntity` | `localSiteId` | `LocalId` | Not possible | Yes |
| `ProductVariationEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `RefundEntity` | `siteId` | `LocalId` | Not possible | No |
| `ShippingMethodEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `TaxBasedOnSettingEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `TaxRateEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `TopPerformerProductEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `VisitorSummaryStatsEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WCSettingsModel` | `localSiteId` | `LocalId` | Not possible | No |
| `WooPaymentsDepositsOverviewEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooShippingLabelEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooShippingPackagesEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooShippingShipmentEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooPosProductEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooPosVariationEntity` | `localSiteId` | `LocalId` | Not possible | No |
| `WooPosSearchableFtsEntity` | `localSiteId` | `String` | Not possible | No |

## WP Database (`wp-android-database`) — Entities with Site ID

| Entity | Column | Type | ID Type | FK to SiteEntity | Why |
|--------|--------|------|---------|------------------|-----|
| `NotificationEntity` | `remoteSiteId` | `RemoteId` | Remote | No | Remote ID, not local |
| `BlazeCampaignEntity` | `siteId` | `Long` | Remote | No | Remote ID, not local |
| `DomainEntity` | `siteLocalId` | `Int` | Local | Possible | |
| `SitePluginModel` | `siteId` | `LocalId` | Local | Possible | |
| `ThemeModel` | `siteId` | `LocalId` | Local | Possible | |

## Cascade Delete

Since FK constraints with `ON DELETE CASCADE` are not available for WC entities, cascade delete behavior must be handled programmatically. When a site is removed, the application code should explicitly delete all related WC data for that site.

See `SiteStore.removeSite()` and `SiteStore.removeAllSites()` for the current implementation. These methods currently only delete the `SiteEntity` row — they do **not** cascade to WC entities. If cascade behavior is needed, it must be added there by calling the appropriate WC DAOs.

## Possible Solutions

1. **Programmatic cascade delete** — Add explicit delete calls in `SiteStore.removeSite()` and `SiteStore.removeAllSites()` to clean up WC entities. This is the simplest approach but requires maintaining the list of DAOs to call.

2. **Merge databases** — Combine `wp-android-database` and `wc-android-database` into a single database. This would restore the ability to use FK constraints but is a significant architectural change.

3. **Move SiteEntity** — Move `SiteEntity` from `WPAndroidDatabase` to `WCAndroidDatabase`. Similar trade-offs to merging databases.

4. **Accept orphaned data** — WC entity data for deleted sites is harmless (never queried without a valid site ID) and will be overwritten if the site is re-added. This is the current behavior.
