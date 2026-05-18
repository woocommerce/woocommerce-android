package com.woocommerce.android.aiassistant.tools.handlers.cards

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

internal sealed interface ShowCardsResolvedSummary {
    data class Order(val value: OrderSummary) : ShowCardsResolvedSummary
    data class Product(val value: ProductSummary) : ShowCardsResolvedSummary
    data class Variation(val value: VariationSummary) : ShowCardsResolvedSummary
    data class AnalyticsStats(val value: AnalyticsStatsSummary) : ShowCardsResolvedSummary
    data class Customer(val value: CustomerSummary) : ShowCardsResolvedSummary
}

internal fun ShowCardsResolvedSummary.toJsonObject(json: Json): JsonObject =
    when (this) {
        is ShowCardsResolvedSummary.Order -> json.encodeToJsonElement(value).jsonObject
        is ShowCardsResolvedSummary.Product -> json.encodeToJsonElement(value).jsonObject
        is ShowCardsResolvedSummary.Variation -> json.encodeToJsonElement(value).jsonObject
        is ShowCardsResolvedSummary.AnalyticsStats -> json.encodeToJsonElement(value).jsonObject
        is ShowCardsResolvedSummary.Customer -> json.encodeToJsonElement(value).jsonObject
    }
