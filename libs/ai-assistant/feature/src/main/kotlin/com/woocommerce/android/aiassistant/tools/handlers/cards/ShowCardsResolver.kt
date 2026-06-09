package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.analytics.AIAnalyticsDataSource
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsStats
import com.woocommerce.android.aiassistant.tools.analytics.analyticsDateAfterBound
import com.woocommerce.android.aiassistant.tools.analytics.analyticsDateBeforeBound
import com.woocommerce.android.aiassistant.tools.analytics.analyticsStatsSummary
import com.woocommerce.android.aiassistant.tools.customers.AICustomersDataSource
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.orders.CompactOrderLineItem
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import com.woocommerce.android.aiassistant.tools.products.CompactVariationAttribute
import com.woocommerce.android.aiassistant.tools.products.toProductVariationDetailResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
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
        val summary: ShowCardsResolvedSummary,
        val card: ShowCardPayload,
    ) : ShowCardsResolution

    data class Missing(
        override val ref: ValidatedRef,
        val reason: ShowCardsRejectionReason,
    ) : ShowCardsResolution
}

internal class DefaultShowCardsResolver @Inject constructor(
    private val ordersDataSource: AIOrdersDataSource,
    private val productsDataSource: com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource,
    private val variationsDataSource: AIProductVariationsDataSource,
    private val analyticsDataSource: AIAnalyticsDataSource,
    private val customersDataSource: AICustomersDataSource,
) : ShowCardsResolver {
    override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> {
        val orderResults = resolveOrders(refs.filter { it.family == ShowCardFamily.Order })
        val productResults = resolveProducts(refs.filter { it.family == ShowCardFamily.Product })
        val variationResults = resolveVariations(refs.filter { it.family == ShowCardFamily.Variation })
        val analyticsResults = resolveAnalyticsStats(refs.filter { it.family == ShowCardFamily.AnalyticsStats })
        val customerResults = resolveCustomers(refs.filter { it.family == ShowCardFamily.Customer })

        return refs.map { ref ->
            orderResults[ref]
                ?: productResults[ref]
                ?: variationResults[ref]
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
            summary = ShowCardsResolvedSummary.Order(
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
        summary = ShowCardsResolvedSummary.Product(
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

    private suspend fun resolveVariations(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        return coroutineScope {
            refs.map { ref -> async { ref to resolveVariation(ref) } }.awaitAll().toMap()
        }
    }

    private suspend fun resolveVariation(ref: ValidatedRef): ShowCardsResolution {
        val id = VariationCardId.parse(ref.id)
            ?: return ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.InvalidId)

        return variationsDataSource.getVariation(
            productId = id.productId,
            variationId = id.variationId,
        ).fold(
            onSuccess = { variation ->
                val parentProductName = runCatching {
                    productsDataSource.getProduct(id.productId)
                        .getOrNull()
                        ?.name
                        ?.takeIf { it.isNotBlank() }
                }.getOrNull()

                variation.toResolved(ref, parentProductName)
            },
            onFailure = { ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed) },
        )
    }

    private fun WCProductVariationModel.toResolved(
        ref: ValidatedRef,
        parentProductName: String?,
    ): ShowCardsResolution.Resolved {
        val detail = toProductVariationDetailResponse()
        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = ShowCardsResolvedSummary.Variation(
                VariationSummary(
                    id = ref.id,
                    productId = detail.productId,
                    variationId = detail.id,
                    sku = detail.sku.takeIf { it.isNotBlank() },
                    price = detail.price.takeIf { it.isNotBlank() },
                    stockStatus = detail.stockStatus.takeIf { it.isNotBlank() },
                    status = detail.status.takeIf { it.isNotBlank() },
                    attributes = detail.attributes,
                )
            ),
            card = ShowCardPayload(
                family = ShowCardFamily.Variation.serializedName,
                id = ref.id,
                title = detail.attributes.toVariationTitle()
                    ?: detail.sku.takeIf { it.isNotBlank() }
                    ?: "Variation ${detail.id}",
                details = ShowCardDetails.Variation(
                    productId = detail.productId,
                    variationId = detail.id,
                    parentProductName = parentProductName,
                    sku = detail.sku.takeIf { it.isNotBlank() },
                    price = detail.price.takeIf { it.isNotBlank() },
                    stockStatus = detail.stockStatus.takeIf { it.isNotBlank() },
                    status = detail.status.takeIf { it.isNotBlank() },
                    imageUrl = detail.image?.src?.takeIf { it.isNotBlank() },
                    attributes = detail.attributes,
                ),
            ),
        )
    }

    private fun List<CompactVariationAttribute>.toVariationTitle(): String? =
        mapNotNull { attribute ->
            val option = attribute.option?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = attribute.name?.takeIf { it.isNotBlank() }
            if (name != null) "$name: $option" else option
        }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " \u2022 ")

    private suspend fun resolveAnalyticsStats(refs: List<ValidatedRef>): Map<ValidatedRef, ShowCardsResolution> {
        if (refs.isEmpty()) return emptyMap()

        return coroutineScope {
            refs.map { ref -> async { ref to resolveAnalyticsStats(ref) } }.awaitAll().toMap()
        }
    }

    private suspend fun resolveAnalyticsStats(ref: ValidatedRef): ShowCardsResolution {
        val query = AnalyticsStatsCardId.parse(ref.id)
            ?: return ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.InvalidId)
        val after = analyticsDateAfterBound(query.after)
        val before = analyticsDateBeforeBound(query.before)
        val statsResult = analyticsDataSource.fetchOrdersStats(
            after = after,
            before = before,
            interval = query.interval,
        )

        return statsResult.fold(
            onSuccess = { stats ->
                analyticsStatsResolution(ref = ref, query = query, stats = stats)
            },
            onFailure = {
                ShowCardsResolution.Missing(ref, ShowCardsRejectionReason.FetchFailed)
            },
        )
    }

    private fun analyticsStatsResolution(
        ref: ValidatedRef,
        query: AnalyticsStatsCardId,
        stats: AnalyticsStats,
    ): ShowCardsResolution.Resolved {
        val displayCurrency = analyticsDataSource.getSelectedSiteCurrencyCode()
        val summary = analyticsStatsSummary(
            after = query.after,
            before = query.before,
            interval = query.interval,
            stats = stats,
            cardId = ref.id,
            currency = displayCurrency,
        )
        val totals = summary["totals"] as? JsonObject ?: JsonObject(emptyMap())
        val intervalSubtotals = (summary["interval_subtotals"] as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            .orEmpty()

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = ShowCardsResolvedSummary.AnalyticsStats(
                AnalyticsStatsSummary(
                    id = ref.id,
                    after = query.after,
                    before = query.before,
                    currency = displayCurrency,
                    totals = totals,
                    intervalSubtotals = intervalSubtotals,
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
                ),
            ),
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

    private fun WCCustomerModel.toResolved(ref: ValidatedRef): ShowCardsResolution.Resolved {
        val displayName = displayName(ref.id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = ShowCardsResolvedSummary.Customer(
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
