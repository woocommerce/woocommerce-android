package com.woocommerce.android.aiassistant.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

private val displayNameJson = Json { ignoreUnknownKeys = true }

internal fun OrderEntity.confirmationDisplayName(): String? =
    listOf(billingFirstName, billingLastName)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .takeIf { it.isNotEmpty() }

internal fun WCProductModel.confirmationDisplayName(): String? =
    name.trim().takeIf { it.isNotEmpty() }

internal fun WCProductVariationModel.confirmationDisplayName(): String? =
    variationAttributeOptions().takeIf { it.isNotEmpty() }
        ?: sku.trim().takeIf { it.isNotEmpty() }

private fun WCProductVariationModel.variationAttributeOptions(): String =
    runCatching {
        displayNameJson.decodeFromString<List<VariationAttribute>>(attributes)
            .mapNotNull { it.option?.trim()?.takeIf(String::isNotEmpty) }
            .joinToString(", ")
    }.getOrDefault("")

@Serializable
private data class VariationAttribute(
    val option: String? = null,
)
