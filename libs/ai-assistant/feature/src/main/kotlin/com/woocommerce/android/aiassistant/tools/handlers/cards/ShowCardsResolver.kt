package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.analytics.AIAnalyticsDataSource
import com.woocommerce.android.aiassistant.tools.analytics.analyticsDateAfterBound
import com.woocommerce.android.aiassistant.tools.analytics.analyticsDateBeforeBound
import com.woocommerce.android.aiassistant.tools.analytics.analyticsStatsSummary
import com.woocommerce.android.aiassistant.tools.customers.AICustomersDataSource
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.orders.CompactOrderLineItem
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.LineItem
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
    private val analyticsDataSource: AIAnalyticsDataSource,
    private val customersDataSource: AICustomersDataSource,
    @AiAssistantJson private val json: Json,
) : ShowCardsResolver {
    override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> {
        val orderResults = resolveOrders(refs.filter { it.family == ShowCardFamily.Order })
        val productResults = resolveProducts(refs.filter { it.family == ShowCardFamily.Product })
        val analyticsResults = resolveAnalyticsStats(refs.filter { it.family == ShowCardFamily.AnalyticsStats })
        val customerResults = resolveCustomers(refs.filter { it.family == ShowCardFamily.Customer })

        return refs.map { ref ->
            orderResults[ref]
                ?: productResults[ref]
                ?: analyticsResults[ref]
                ?: customerResults[ref]
                ?: ShowCardsResolution.Missing(
                    ref = ref,
                    reason = ShowCardsRejectionReason.NotFound,
                )
        }
    }

    private suspend fun resolveOrders(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        val ids = refs.map { it.id.toLong() }
        val orders = ordersDataSource.getOrders(ids).getOrElse {
            return refs.associateWith { ref ->
                ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed)
            }
        }
        val ordersById = orders.items.associateBy { it.orderId }
        return buildMap {
            for (ref in refs) {
                put(
                    ref,
                    ordersById[ref.id.toLong()]?.toResolved(ref)
                        ?: ShowCardsResolution.Missing(ref, orders.missingReason())
                )
            }
        }
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

    private suspend fun OrderEntity.toResolved(ref: ValidatedRef): ShowCardsResolution.Resolved {
        val lineItems = getLineItemList()
        return ShowCardsResolution.Resolved(
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
                    paymentMethodTitle = paymentMethodTitle.takeIf { it.isNotBlank() },
                    customerId = customerId.takeIf { it > 0L },
                    lineItemsCount = lineItems.size,
                    lineItems = lineItems.take(SHOW_CARDS_LINE_ITEMS_LIMIT).map { it.toCompactLineItem() },
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
    }

    private fun WCProductModel.toResolved(ref: ValidatedRef) = ShowCardsResolution.Resolved(
        ref = ref,
        summary = jsonObject(
            ProductSummary(
                id = ref.id,
                name = name,
                sku = sku,
                price = price,
                type = type,
                stockStatus = stockStatus,
                manageStock = manageStock,
                onSale = onSale,
                stockQuantity = stockQuantity,
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
                imageUrl = getFirstImageUrl()?.takeIf { it.isNotBlank() },
            ),
        ),
    )

    private suspend fun resolveAnalyticsStats(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        return buildMap {
            refs.forEach { ref ->
                put(ref, resolveAnalyticsStats(ref))
            }
        }
    }

    private suspend fun resolveAnalyticsStats(ref: ValidatedRef): ShowCardsResolution {
        val query = AnalyticsStatsCardId.parse(ref.id)
            ?: return ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.InvalidId)
        val after = analyticsDateAfterBound(query.after)
        val before = analyticsDateBeforeBound(query.before)
        val statsResult = when (query.kind) {
            AnalyticsStatsKind.Revenue -> analyticsDataSource.fetchRevenueStats(
                after = after,
                before = before,
                interval = query.interval,
                currency = query.currency,
            )
            AnalyticsStatsKind.Orders -> analyticsDataSource.fetchOrdersStats(
                after = after,
                before = before,
                interval = query.interval,
            )
        }

        return statsResult.fold(
            onSuccess = { stats ->
                val displayCurrency = query.currency ?: analyticsDataSource.getSelectedSiteCurrencyCode()
                val summary = analyticsStatsSummary(
                    after = query.after,
                    before = query.before,
                    interval = query.interval,
                    stats = stats,
                    currency = displayCurrency,
                )
                val totals = summary["totals"] as? JsonObject ?: JsonObject(emptyMap())
                val intervalSubtotals = (summary["interval_subtotals"] as? JsonArray)
                    ?.mapNotNull { it as? JsonObject }
                    .orEmpty()
                ShowCardsResolution.Resolved(
                    ref = ref,
                    summary = jsonObject(
                        AnalyticsStatsSummary(
                            id = ref.id,
                            after = query.after,
                            before = query.before,
                            currency = displayCurrency,
                            totals = totals,
                            intervalSubtotals = intervalSubtotals,
                            kind = query.kind.serializedName,
                        )
                    ),
                    card = ShowCardPayload(
                        family = ShowCardFamily.AnalyticsStats.serializedName,
                        id = ref.id,
                        title = "Analytics",
                        details = ShowCardDetails.AnalyticsStats(
                            after = query.after,
                            before = query.before,
                            currency = displayCurrency,
                            totals = totals,
                            intervalSubtotals = intervalSubtotals,
                            kind = query.kind.serializedName,
                        ),
                    ),
                )
            },
            onFailure = {
                ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed)
            },
        )
    }

    private suspend fun resolveCustomers(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        val ids = refs.map { it.id.toLong() }
        return customersDataSource.fetchCustomers(
            search = null,
            email = null,
            include = ids,
            orderby = "registered_date",
            order = "desc",
            page = null,
            perPage = ids.size,
        ).fold(
            onSuccess = { customers ->
                val customersById = customers.associateBy { it.remoteCustomerId.value }
                refs.associateWith { ref ->
                    customersById[ref.id.toLong()]?.toResolved(ref)
                        ?: ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.NotFound)
                }
            },
            onFailure = {
                refs.associateWith { ref -> ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed) }
            },
        )
    }

    private inline fun <reified T> jsonObject(value: T): JsonObject =
        json.encodeToJsonElement(value).jsonObject

    private fun WCCustomerModel.toResolved(ref: ValidatedRef): ShowCardsResolution.Resolved {
        val displayName = displayName(ref.id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = jsonObject(
                CustomerSummary(
                    id = ref.id,
                    name = displayName,
                    email = email.takeIf { it.isNotBlank() },
                )
            ),
            card = ShowCardPayload(
                family = ShowCardFamily.Customer.serializedName,
                id = ref.id,
                title = displayName,
                details = ShowCardDetails.Customer(
                    email = email.takeIf { it.isNotBlank() },
                ),
            ),
        )
    }

    private fun WCCustomerModel.displayName(id: String): String =
        listOf(firstName, lastName)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { email }
            .ifBlank { username }
            .ifBlank { "Customer $id" }
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

private fun LineItem.toCompactLineItem() = CompactOrderLineItem(
    id = id,
    name = name,
    quantity = quantity,
    sku = sku,
    total = total,
    productId = productId,
    variationId = variationId,
)

private fun CachedLookupResult<*>.missingReason(): ShowCardsRejectionReason =
    if (fetchFailed) ShowCardsRejectionReason.FetchFailed else ShowCardsRejectionReason.NotFound

private const val SHOW_CARDS_LINE_ITEMS_LIMIT = 5
