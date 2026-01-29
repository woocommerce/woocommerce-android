package org.wordpress.android.fluxc.persistence.entity.pos

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 virtual table for full-text search of products and variations.
 *
 * Note: All fields use String type because FTS4 tables in SQLite only support TEXT columns.
 * Room's @Fts4 annotation officially supports only String fields.
 * See: https://developer.android.com/reference/androidx/room/Fts4
 *
 * - [parentProductId]: Empty string "" means this is a product, non-empty means it's a variation
 *   (containing the parent product ID). We use empty string instead of nullable because
 *   FTS virtual tables handle NULL inconsistently.
 * - [name]: Product name, or parent product name for variations.
 * - [sku]: Product/variation SKU.
 * - [barcode]: Product/variation globalUniqueId (barcode).
 * - [attributeValues]: Space-separated attribute values for variations (e.g., "blue medium").
 *   Empty string for products.
 */
@Entity(tableName = "PosSearchableFts")
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class WooPosSearchableFtsEntity(
    val localSiteId: String,
    val itemId: String,
    val parentProductId: String,
    val name: String,
    val sku: String,
    val barcode: String,
    val attributeValues: String,
)
