package com.woocommerce.android.aiassistant.appfunctions

import android.app.PendingIntent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionNotSupportedException
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.orders.OrderDetailResponse
import com.woocommerce.android.aiassistant.tools.orders.OrderListRowResponse
import com.woocommerce.android.aiassistant.tools.products.ProductDetailResponse
import com.woocommerce.android.aiassistant.tools.products.ProductListRowResponse
import com.woocommerce.android.aiassistant.tools.products.ProductVariationDetailResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.SiteUtils
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

internal const val PERIOD_TODAY = "today"
internal const val PERIOD_YESTERDAY = "yesterday"
internal const val PERIOD_LAST_7_DAYS = "last_7_days"
internal const val PERIOD_THIS_MONTH = "this_month"

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
internal class StoreAppFunctions @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val storeAccess: AppFunctionsStoreAccess,
    private val wooCommerceStore: WooCommerceStore,
    private val intents: StoreAppFunctionIntents,
    @AiAssistantJson private val json: Json,
) {
    suspend fun getSalesSummary(period: String, now: Instant = Instant.now()): SalesSummary {
        val store = requireAllowedStore()
        val storeName = store.storeName()
        val today = now.atZone(SiteUtils.getNormalizedTimezone(store.timezone).toZoneId()).toLocalDate()
        val (start, end) = when (period) {
            PERIOD_TODAY -> today to today
            PERIOD_YESTERDAY -> today.minusDays(1) to today.minusDays(1)
            PERIOD_LAST_7_DAYS -> today.minusDays(LAST_7_DAYS_START_OFFSET) to today
            PERIOD_THIS_MONTH -> today.withDayOfMonth(1) to today
            else -> throw AppFunctionInvalidArgumentException(
                "period must be today, yesterday, last_7_days or this_month."
            )
        }
        val summary = execute(
            call = toolCall(ANALYTICS_ORDERS) {
                put("after", start.toString())
                put("before", end.toString())
                put("interval", "day")
            },
            failureMessage = "Couldn't load sales for $storeName. Try again in a moment.",
        )
        val currency = currencyOf(store)
        requireSameStore(store)
        val totals = summary["totals"] as? JsonObject
        return SalesSummary(
            storeName = storeName,
            period = period,
            startDate = start.toString(),
            endDate = end.toString(),
            currency = currency,
            netSales = totals?.text("net_revenue"),
            orderCount = totals?.count("orders_count"),
            averageOrderValue = totals?.text("avg_order_value"),
            itemsSold = totals?.count("num_items_sold"),
        )
    }

    suspend fun findOrders(searchText: String?, status: String?, limit: Int?): OrderSearchResult {
        val store = requireAllowedStore()
        val storeName = store.storeName()
        val pageSize = limit.toPageSize()
        val response = execute(
            call = toolCall(ORDERS_LIST) {
                put("per_page", pageSize)
                searchText?.takeIf { it.isNotBlank() }?.let { put("search", it) }
                status?.let { put("status", it) }
            },
            failureMessage = "Couldn't search orders in $storeName. Try again in a moment.",
        )
        requireSameStore(store)
        val rows = json.decodeFromJsonElement<List<OrderListRowResponse>>(response.getValue("orders"))
        return OrderSearchResult(
            storeName = storeName,
            orders = rows.map { it.toOrderSummary() },
            mayHaveMore = rows.size >= pageSize,
        )
    }

    suspend fun getOrderDetails(orderId: Long): OrderDetails {
        val store = requireAllowedStore()
        val order = json.decodeFromJsonElement<OrderDetailResponse>(requireOrder(orderId, store))
        requireSameStore(store)
        return order.toOrderDetails(store.storeName())
    }

    suspend fun findProducts(
        searchText: String?,
        sku: String?,
        stockStatus: String?,
        limit: Int?,
    ): ProductSearchResult {
        val store = requireAllowedStore()
        val storeName = store.storeName()
        val pageSize = limit.toPageSize()
        val response = execute(
            call = toolCall(PRODUCTS_LIST) {
                put("per_page", pageSize)
                searchText?.takeIf { it.isNotBlank() }?.let { put("search", it) }
                sku?.takeIf { it.isNotBlank() }?.let { put("sku", it) }
                stockStatus?.let { put("stock_status", it) }
            },
            failureMessage = "Couldn't search products in $storeName. Try again in a moment.",
        )
        val currency = currencyOf(store)
        requireSameStore(store)
        val rows = json.decodeFromJsonElement<List<ProductListRowResponse>>(response.getValue("products"))
        return ProductSearchResult(
            storeName = storeName,
            currency = currency,
            products = rows.map { it.toProductSummary() },
            mayHaveMore = response["can_load_more"]?.jsonPrimitive?.booleanOrNull ?: false,
        )
    }

    suspend fun requestStockChange(productId: Long, variationId: Long?, newQuantity: Int): PendingIntent {
        if (newQuantity < 0) throw AppFunctionInvalidArgumentException("newQuantity must be 0 or more.")
        val store = requireAllowedStore()
        val product = json.decodeFromJsonElement<ProductDetailResponse>(requireProduct(productId, store))
        val call = if (variationId == null) {
            simpleProductStockCall(product, newQuantity, store)
        } else {
            requireVariation(productId, variationId, store)
            toolCall(PRODUCT_VARIATIONS_UPDATE) {
                put("product_id", productId)
                put("id", variationId)
                put("stock_quantity", newQuantity)
            }
        }
        requireSameStore(store)
        return intents.confirmation(call, store.identity(), store.storeName())
    }

    suspend fun openOrderInApp(orderId: Long): PendingIntent {
        val store = requireAllowedStore()
        requireOrder(orderId, store)
        requireSameStore(store)
        return intents.openOrder(orderId, store.identity(), store.storeName())
    }

    private fun requireAllowedStore(): SiteModel =
        storeAccess.allowedStore() ?: throw AppFunctionNotSupportedException(NOT_AVAILABLE_MESSAGE)

    private fun requireSameStore(store: SiteModel) {
        when (storeAccess.check(store.identity())) {
            StoreCheck.SAME_STORE -> Unit
            StoreCheck.DIFFERENT_STORE -> throw AppFunctionNotSupportedException(STORE_CHANGED_MESSAGE)
            StoreCheck.NO_ACCESS -> throw AppFunctionNotSupportedException(NOT_AVAILABLE_MESSAGE)
        }
    }

    private suspend fun requireOrder(orderId: Long, store: SiteModel): JsonObject = lookup(
        store = store,
        call = toolCall(ORDERS_GET) { put("id", orderId) },
        existenceCheck = includeOnly(ORDERS_LIST, orderId),
        notFoundMessage = "There is no order with ID $orderId in ${store.storeName()}. " +
            "If $orderId is an order number, find the order with findOrders first.",
        failureMessage = "Couldn't load order $orderId from ${store.storeName()}. Try again in a moment.",
    )

    private suspend fun requireProduct(productId: Long, store: SiteModel): JsonObject = lookup(
        store = store,
        call = toolCall(PRODUCTS_GET) { put("id", productId) },
        existenceCheck = includeOnly(PRODUCTS_LIST, productId),
        notFoundMessage = "There is no product with ID $productId in ${store.storeName()}. " +
            "Use findProducts to get the product ID.",
        failureMessage = "Couldn't load product $productId from ${store.storeName()}. Try again in a moment.",
    )

    private suspend fun requireVariation(productId: Long, variationId: Long, store: SiteModel) {
        execute(
            call = toolCall(PRODUCT_VARIATIONS_LIST) {
                put("product_id", productId)
                put("variation_id", variationId)
            },
            failureMessage = "Couldn't load variation $variationId of product $productId from " +
                "${store.storeName()}. Check both IDs and try again.",
        )
    }

    private suspend fun simpleProductStockCall(
        product: ProductDetailResponse,
        newQuantity: Int,
        store: SiteModel,
    ): ToolCall = when {
        product.type.isBlank() || product.type == SIMPLE_PRODUCT_TYPE -> toolCall(PRODUCTS_UPDATE) {
            put("id", product.id)
            put("stock_quantity", newQuantity)
        }
        product.type == VARIABLE_PRODUCT_TYPE ->
            throw AppFunctionInvalidArgumentException(variationChoicesMessage(product, store))
        else -> throw AppFunctionInvalidArgumentException(
            "${product.name} (product ${product.id}) is a ${product.type} product. Only simple products and " +
                "variations of variable products can have their stock changed."
        )
    }

    private suspend fun variationChoicesMessage(product: ProductDetailResponse, store: SiteModel): String {
        val list = execute(
            call = toolCall(PRODUCT_VARIATIONS_LIST) { put("product_id", product.id) },
            failureMessage = "Couldn't load the variations of product ${product.id} from ${store.storeName()}. " +
                "Try again in a moment.",
        )
        requireSameStore(store)
        val variations = json.decodeFromJsonElement<List<ProductVariationDetailResponse>>(list.getValue("variations"))
        val choices = variations.joinToString { variation ->
            val options = variation.attributes.mapNotNull { it.option?.takeIf(String::isNotBlank) }
            if (options.isEmpty()) "${variation.id}" else "${variation.id} (${options.joinToString(", ")})"
        }
        return "${product.name} (product ${product.id}) has variations, and stock is set per variation. Ask the " +
            "merchant which variation to change, then call requestStockChange again with its variationId. " +
            "Variations (up to 20): $choices."
    }

    private suspend fun lookup(
        store: SiteModel,
        call: ToolCall,
        existenceCheck: ToolCall,
        notFoundMessage: String,
        failureMessage: String,
    ): JsonObject {
        val result = toolRegistry.execute(call)
        if (result is ToolResult.TransportError && isAbsent(existenceCheck)) {
            requireSameStore(store)
            throw AppFunctionElementNotFoundException(notFoundMessage)
        }
        return result.structuredOrThrow(failureMessage)
    }

    private suspend fun isAbsent(listCall: ToolCall): Boolean =
        (toolRegistry.execute(listCall) as? ToolResult.Success)
            ?.structured?.jsonObject?.get("count")?.jsonPrimitive?.intOrNull == 0

    private suspend fun execute(call: ToolCall, failureMessage: String): JsonObject =
        toolRegistry.execute(call).structuredOrThrow(failureMessage)

    private fun ToolResult.structuredOrThrow(failureMessage: String): JsonObject = when (this) {
        is ToolResult.Success -> structured.jsonObject
        is ToolResult.ValidationError -> throw AppFunctionInvalidArgumentException(reason)
        is ToolResult.TransportError,
        is ToolResult.RejectedBySafety -> throw AppFunctionAppUnknownException(failureMessage)
    }

    private suspend fun currencyOf(store: SiteModel): String? =
        wooCommerceStore.getSiteSettings(store)?.currencyCode?.takeIf { it.isNotBlank() }

    private fun includeOnly(toolName: String, id: Long) = toolCall(toolName) {
        putJsonArray("include") { add(id) }
        put("per_page", 1)
    }

    private fun toolCall(name: String, arguments: JsonObjectBuilder.() -> Unit) =
        ToolCall(id = "appfunctions-${UUID.randomUUID()}", name = name, arguments = buildJsonObject(arguments))

    private fun Int?.toPageSize(): Int = (this?.takeIf { it > 0 } ?: DEFAULT_LIMIT).coerceAtMost(MAX_LIMIT)

    private fun SiteModel.storeName(): String =
        sequenceOf(displayName, name, url).firstOrNull { !it.isNullOrBlank() }.orEmpty()

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.content

    private fun JsonObject.count(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun OrderListRowResponse.toOrderSummary() = OrderSummary(
        orderId = id,
        orderNumber = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        customerName = customerName,
    )

    private fun OrderDetailResponse.toOrderDetails(storeName: String) = OrderDetails(
        storeName = storeName,
        orderId = id,
        orderNumber = number,
        status = status,
        dateCreated = dateCreated,
        datePaid = datePaid.takeIf { it.isNotBlank() },
        total = total,
        currency = currency,
        shippingTotal = shippingTotal,
        discountTotal = discountTotal,
        totalTax = totalTax,
        customerName = customerName,
        itemCount = lineItemsCount,
        items = lineItems.take(ORDER_ITEM_LIMIT).map { item ->
            OrderItem(
                name = item.name.orEmpty(),
                quantity = item.quantity ?: 0f,
                total = item.total.orEmpty(),
                productId = item.productId?.takeIf { it != 0L },
                variationId = item.variationId?.takeIf { it != 0L },
            )
        },
    )

    private fun ProductListRowResponse.toProductSummary() = ProductSummary(
        productId = id,
        name = name,
        sku = sku.takeIf { it.isNotBlank() },
        type = type,
        status = status,
        price = price,
        stockStatus = stockStatus,
        stockQuantity = stockQuantity?.takeIf { manageStock == true }?.toInt(),
    )

    private companion object {
        const val ANALYTICS_ORDERS = "analytics_orders"
        const val ORDERS_LIST = "orders_list"
        const val ORDERS_GET = "orders_get"
        const val PRODUCTS_LIST = "products_list"
        const val PRODUCTS_GET = "products_get"
        const val PRODUCTS_UPDATE = "products_update"
        const val PRODUCT_VARIATIONS_LIST = "product_variations_list"
        const val PRODUCT_VARIATIONS_UPDATE = "product_variations_update"
        const val SIMPLE_PRODUCT_TYPE = "simple"
        const val VARIABLE_PRODUCT_TYPE = "variable"
        const val DEFAULT_LIMIT = 10
        const val MAX_LIMIT = 50
        const val LAST_7_DAYS_START_OFFSET = 6L
        const val ORDER_ITEM_LIMIT = 10
        const val NOT_AVAILABLE_MESSAGE = "WooCommerce can't do this right now. The merchant needs to open " +
            "WooCommerce, log in to a store and turn on Assistant access in Settings > Experimental features, " +
            "then try again."
        const val STORE_CHANGED_MESSAGE = "The store open in WooCommerce changed while this request ran, so " +
            "nothing was returned. Ask the merchant which store to use, then try again."
    }
}
