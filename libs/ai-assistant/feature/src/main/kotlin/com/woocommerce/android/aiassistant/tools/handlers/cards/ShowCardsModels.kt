package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal const val MAX_SHOW_CARDS_REFS = 10

@Serializable
internal data class ShowCardsArguments(
    val references: List<JsonElement> = emptyList()
)

internal enum class ShowCardFamily(val serializedName: String) {
    Order("order"),
    Product("product");

    companion object {
        fun from(value: String): ShowCardFamily? =
            entries.firstOrNull { it.serializedName == value }
    }
}

internal data class ValidatedRef(
    val index: Int,
    val family: ShowCardFamily,
    val id: String,
)

@Serializable
internal data class ShowCardsStructured(
    val requested: Int,
    val validated: Int,
    val rendered: Int,
    @SerialName("resolved_refs") val resolvedRefs: List<ResolvedRef>,
    @SerialName("missing_refs") val missingRefs: List<MissingRef>,
    @SerialName("rejected_refs") val rejectedRefs: List<RejectedRef>,
)

@Serializable
internal data class ResolvedRef(
    val family: String,
    val id: String,
    val summary: JsonObject,
)

@Serializable
internal data class OrderSummary(
    val id: String,
    val number: String? = null,
    val status: String? = null,
    val total: String? = null,
    val currency: String? = null,
    @SerialName("date_created") val dateCreated: String? = null,
)

@Serializable
internal data class ProductSummary(
    val id: String,
    val name: String? = null,
    val sku: String? = null,
    val price: String? = null,
    @SerialName("stock_status") val stockStatus: String? = null,
)

@Serializable
internal data class MissingRef(
    val family: String,
    val id: String,
    val reason: ShowCardsRejectionReason,
)

@Serializable
internal data class RejectedRef(
    val index: Int,
    val family: String? = null,
    val id: String? = null,
    val reason: ShowCardsRejectionReason,
)

@Serializable
internal enum class ShowCardsRejectionReason {
    @SerialName("malformed_ref") MalformedRef,
    @SerialName("missing_family") MissingFamily,
    @SerialName("unsupported_family") UnsupportedFamily,
    @SerialName("missing_id") MissingId,
    @SerialName("invalid_id") InvalidId,
    @SerialName("duplicate_ref") DuplicateRef,
    @SerialName("over_limit") OverLimit,
    @SerialName("fetch_failed") FetchFailed,
    @SerialName("not_found") NotFound
}

@Serializable
internal data class ShowCardsUiStructured(
    val cards: List<ShowCardPayload>,
)

@Serializable
internal data class ShowCardPayload(
    val family: String,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val badges: List<String> = emptyList(),
    val attributes: Map<String, String> = emptyMap(),
)
