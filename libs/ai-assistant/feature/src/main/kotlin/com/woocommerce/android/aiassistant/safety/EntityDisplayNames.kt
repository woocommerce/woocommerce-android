package com.woocommerce.android.aiassistant.safety

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

private val displayNameJson = Json { ignoreUnknownKeys = true }

internal fun OrderEntity.confirmationDisplayName(
    guestDisplayName: () -> String,
    customerDisplayName: (Long) -> String,
): String? =
    billingName()
        ?: when {
            customerId == GUEST_CUSTOMER_ID -> guestDisplayName()
            customerId > GUEST_CUSTOMER_ID -> registeredCustomerDisplayName(customerDisplayName)
            else -> null
        }

private fun OrderEntity.billingName(): String? =
    listOf(billingFirstName, billingLastName)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")
        .takeIf { it.isNotEmpty() }

private fun OrderEntity.registeredCustomerDisplayName(customerDisplayName: (Long) -> String): String =
    billingEmail.trim().takeIf { it.isNotEmpty() }
        ?: customerDisplayName(customerId)

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

private const val GUEST_CUSTOMER_ID = 0L
