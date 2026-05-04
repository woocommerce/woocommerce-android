package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import javax.inject.Inject

internal interface ShowCardsResolver {
    suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution>
}

internal sealed interface ShowCardsResolution {
    val ref: ValidatedRef

    data class Resolved(
        override val ref: ValidatedRef,
        val summary: JsonObject,
        val card: ShowCardPayload,
    ) : ShowCardsResolution

    data class Missing(
        override val ref: ValidatedRef,
        val reason: ShowCardsRejectionReason,
    ) : ShowCardsResolution
}

internal class DefaultShowCardsResolver @Inject constructor(
    private val ordersDataSource: AIOrdersDataSource,
    private val productsDataSource: AIProductsDataSource,
    @AiAssistantJson private val json: Json,
) : ShowCardsResolver {
    override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> {
        val orderResults = resolveOrders(refs.filter { it.family == ShowCardFamily.Order })
        val productResults = resolveProducts(refs.filter { it.family == ShowCardFamily.Product })

        return refs.map { ref ->
            orderResults[ref] ?: productResults[ref] ?: ShowCardsResolution.Missing(
                ref = ref,
                reason = ShowCardsRejectionReason.NotFound,
            )
        }
    }

    private suspend fun resolveOrders(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        val ids = refs.map { it.id.toLong() }
        return ordersDataSource.getOrders(ids).fold(
            onSuccess = { orders ->
                val ordersById = orders.items.associateBy { it.orderId }
                refs.associateWith { ref ->
                    ordersById[ref.id.toLong()]?.toResolved(ref)
                        ?: ShowCardsResolution.Missing(ref, orders.missingReason())
                }
            },
            onFailure = {
                refs.associateWith { ref -> ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed) }
            },
        )
    }

    private suspend fun resolveProducts(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        val ids = refs.map { it.id.toLong() }
        return productsDataSource.getProducts(ids).fold(
            onSuccess = { products ->
                val productsById = products.items.associateBy { it.remoteProductId }
                refs.associateWith { ref ->
                    productsById[ref.id.toLong()]?.toResolved(ref)
                        ?: ShowCardsResolution.Missing(ref, products.missingReason())
                }
            },
            onFailure = {
                refs.associateWith { ref -> ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed) }
            },
        )
    }

    private fun OrderEntity.toResolved(ref: ValidatedRef) = ShowCardsResolution.Resolved(
        ref = ref,
        summary = jsonObject(
            OrderSummary(
                id = ref.id,
                number = number,
                status = status,
                total = total,
                currency = currency,
                dateCreated = dateCreated,
                customerName = customerName,
            )
        ),
        card = ShowCardPayload(
            family = ShowCardFamily.Order.serializedName,
            id = ref.id,
            title = number.toDisplayOrderNumber(orderId),
            details = ShowCardDetails.Order(
                status = status.takeIf { it.isNotBlank() },
                total = total.takeIf { it.isNotBlank() },
                currency = currency.takeIf { it.isNotBlank() },
                dateCreated = dateCreated.takeIf { it.isNotBlank() },
                customerName = customerName.takeIf { it.isNotBlank() },
            ),
        ),
    )

    private fun WCProductModel.toResolved(ref: ValidatedRef) = ShowCardsResolution.Resolved(
        ref = ref,
        summary = jsonObject(
            ProductSummary(
                id = ref.id,
                name = name,
                sku = sku,
                price = price,
                stockStatus = stockStatus,
            )
        ),
        card = ShowCardPayload(
            family = ShowCardFamily.Product.serializedName,
            id = ref.id,
            title = name.ifBlank { "Product $remoteProductId" },
            details = ShowCardDetails.Product(
                sku = sku.takeIf { it.isNotBlank() },
                price = price.takeIf { it.isNotBlank() },
                stockStatus = stockStatus.takeIf { it.isNotBlank() },
                status = status.takeIf { it.isNotBlank() },
            ),
        ),
    )

    private inline fun <reified T> jsonObject(value: T): JsonObject =
        json.encodeToJsonElement(value).jsonObject
}

private fun String.toDisplayOrderNumber(orderId: Long): String {
    val fallback = orderId.toString()
    val value = takeIf { it.isNotBlank() } ?: fallback
    return if (value.startsWith("#")) value else "#$value"
}

private val OrderEntity.customerName: String
    get() = listOf(billingFirstName, billingLastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")

private fun CachedLookupResult<*>.missingReason(): ShowCardsRejectionReason =
    if (fetchFailed) ShowCardsRejectionReason.FetchFailed else ShowCardsRejectionReason.NotFound
