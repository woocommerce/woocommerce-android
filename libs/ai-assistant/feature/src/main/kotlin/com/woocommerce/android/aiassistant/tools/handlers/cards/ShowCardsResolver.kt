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
            )
        ),
        card = ShowCardPayload(
            family = ShowCardFamily.Order.serializedName,
            id = ref.id,
            title = number.toDisplayOrderNumber(orderId),
            subtitle = status.takeIf { it.isNotBlank() },
            badges = listOfNotBlank(status),
            attributes = mapOfNotBlank(
                "total" to total,
                "currency" to currency,
                "date_created" to dateCreated,
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
            subtitle = sku.takeIf { it.isNotBlank() },
            badges = listOfNotBlank(stockStatus, status),
            attributes = mapOfNotBlank(
                "price" to price,
                "stock_status" to stockStatus,
                "status" to status,
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

private fun listOfNotBlank(vararg values: String): List<String> =
    values.filter { it.isNotBlank() }

private fun mapOfNotBlank(vararg pairs: Pair<String, String>): Map<String, String> =
    pairs.filter { (_, value) -> value.isNotBlank() }.toMap()

private fun CachedLookupResult<*>.missingReason(): ShowCardsRejectionReason =
    if (fetchFailed) ShowCardsRejectionReason.FetchFailed else ShowCardsRejectionReason.NotFound
