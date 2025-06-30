package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.utils.NonNegativeDoubleJsonDeserializer
import org.wordpress.android.fluxc.utils.PrimitiveBooleanJsonDeserializer

typealias ProductDto = ProductApiResponse

@Suppress("ConstructorParameterNaming")
data class ProductApiResponse(
    val id: Long? = null,
    val localSiteId: Int = 0,
    val name: String? = null,
    val slug: String? = null,
    val permalink: String? = null,
    val date_created: String? = null,
    val date_modified: String? = null,
    val type: String? = null,
    val status: String? = null,
    val featured: Boolean = false,
    val catalog_visibility: String? = null,
    val description: String? = null,
    val short_description: String? = null,
    val sku: String? = null,
    val global_unique_id: String? = null,
    val price: String? = null,
    val regular_price: String? = null,
    val sale_price: String? = null,
    val on_sale: Boolean = false,
    val total_sales: Long = 0L,
    @JsonAdapter(PrimitiveBooleanJsonDeserializer::class)
    val purchasable: Boolean = false,
    val virtual: Boolean = false,
    val downloadable: Boolean = false,
    val download_limit: Long = 0L,
    val download_expiry: Int = 0,
    val external_url: String? = null,
    val button_text: String? = null,
    val tax_status: String? = null,
    val tax_class: String? = null,
    val manage_stock: String? = null,
    @JsonAdapter(NonNegativeDoubleJsonDeserializer::class)
    val stock_quantity: Double? = 0.0,
    val stock_status: String? = null,
    val date_on_sale_from: String? = null,
    val date_on_sale_to: String? = null,
    val date_on_sale_from_gmt: String? = null,
    val date_on_sale_to_gmt: String? = null,
    val backorders: String? = null,
    val backorders_allowed: Boolean = false,
    val backordered: Boolean = false,
    @JsonAdapter(PrimitiveBooleanJsonDeserializer::class)
    val sold_individually: Boolean = false,
    val weight: String? = null,
    val dimensions: JsonElement? = null,
    val shipping_required: Boolean = false,
    val shipping_taxable: Boolean = false,
    val shipping_class: String? = null,
    val shipping_class_id: Int = 0,
    val reviews_allowed: Boolean = true,
    val average_rating: String? = null,
    val rating_count: Int = 0,
    val parent_id: Long = 0L,
    val menu_order: Int = 0,
    val purchase_note: String? = null,
    val categories: JsonElement? = null,
    val tags: JsonElement? = null,
    val images: JsonElement? = null,
    val attributes: JsonElement? = null,
    val variations: JsonElement? = null,
    val downloads: JsonElement? = null,
    val related_ids: JsonElement? = null,
    val cross_sell_ids: JsonElement? = null,
    val upsell_ids: JsonElement? = null,
    val grouped_products: JsonElement? = null,
    @SerializedName("meta_data")
    val metadata: JsonArray? = null,
    val bundle_stock_quantity: String? = null,
    val bundle_stock_status: String? = null,
    val bundled_items: JsonArray? = null,
    val composite_components: JsonArray? = null,
    val bundle_min_size: String? = null,
    val bundle_max_size: String? = null,
    val min_quantity: String? = null,
    val max_quantity: String? = null,
    val group_of_quantity: String? = null,
    val combine_variations: String? = null,
    @SerializedName("post_password")
    val password: String? = null,
)
