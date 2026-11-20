package com.woocommerce.android.aiassistant.appfunctions

import android.app.PendingIntent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "StoreAppFunctionService",
    appFunctionXmlFileName = "store_app_functions",
)
abstract class BaseStoreAppFunctionService : AppFunctionService() {
    @Inject
    internal lateinit var storeAppFunctions: StoreAppFunctions

    /**
     * Gets sales totals for one period from the WooCommerce store that is open in the app: net sales, number of
     * orders, average order value and items sold. Days follow the store's time zone. The result names the store.
     *
     * @param period The period to total: today, yesterday, last_7_days (the 7 days ending today) or this_month (from
     * the 1st of the current month to today).
     * @return Sales totals for the period, with the store name, the dates covered and the currency.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun getSalesSummary(
        @AppFunctionStringValueConstraint(
            enumValues = ["today", "yesterday", "last_7_days", "this_month"],
        )
        period: String,
    ): SalesSummary = withContext(Dispatchers.IO) { storeAppFunctions.getSalesSummary(period) }

    /**
     * Finds orders in the WooCommerce store that is open in the app, newest first. Search by customer name, order
     * number or other order text, filter by status, or leave both empty for the latest orders. Each order includes
     * the customer's name but no contact details. For sales totals, use getSalesSummary.
     *
     * @param searchText Text to look for in orders, such as a customer name or an order number. Leave empty to list
     * orders without searching.
     * @param status Only return orders with this status.
     * @param limit How many orders to return, from 1 to 50. Defaults to 10.
     * @return The matching orders and the store name.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun findOrders(
        searchText: String? = null,
        @AppFunctionStringValueConstraint(
            enumValues = ["pending", "processing", "on-hold", "completed", "cancelled", "refunded", "failed"],
        )
        status: String? = null,
        limit: Int? = null,
    ): OrderSearchResult = withContext(Dispatchers.IO) { storeAppFunctions.findOrders(searchText, status, limit) }

    /**
     * Gets one order from the WooCommerce store that is open in the app: status, dates, totals, the customer's name
     * and up to 10 ordered items. Contact details, addresses, payment method and notes are not included.
     *
     * @param orderId The order ID, as returned by findOrders. It can differ from the order number the merchant sees.
     * @return The order and the store name.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun getOrderDetails(orderId: Long): OrderDetails =
        withContext(Dispatchers.IO) { storeAppFunctions.getOrderDetails(orderId) }

    /**
     * Finds products in the WooCommerce store that is open in the app, with price, stock status and stock quantity.
     * Search by name, look up an exact SKU, or filter by stock status, for example to list out-of-stock products.
     *
     * @param searchText Text to look for in product names and SKUs. Ignored when sku is set.
     * @param sku An exact SKU to look up.
     * @param stockStatus Only return products with this stock status.
     * @param limit How many products to return, from 1 to 50. Defaults to 10.
     * @return The matching products, the currency of their prices and the store name.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun findProducts(
        searchText: String? = null,
        sku: String? = null,
        @AppFunctionStringValueConstraint(enumValues = ["instock", "outofstock", "onbackorder"])
        stockStatus: String? = null,
        limit: Int? = null,
    ): ProductSearchResult = withContext(Dispatchers.IO) {
        storeAppFunctions.findProducts(searchText, sku, stockStatus, limit)
    }

    /**
     * Asks the merchant to approve a new stock quantity for a product in the WooCommerce app. Nothing changes until
     * the merchant opens the returned confirmation screen and approves the change there. Use findProducts first to
     * get the product ID and the current stock. Works for simple products and for single variations of variable
     * products. Setting a quantity turns on stock tracking for the product or variation.
     *
     * @param productId The product ID, as returned by findProducts.
     * @param variationId For a variable product, the ID of the variation to change. Leave empty for a simple
     * product. If it is unknown, call without it: the error lists the product's variations and their IDs.
     * @param newQuantity The new number of units in stock, 0 or more.
     * @return A confirmation screen in the WooCommerce app. Open it so the merchant can review and approve the change.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun requestStockChange(productId: Long, variationId: Long? = null, newQuantity: Int): PendingIntent =
        withContext(Dispatchers.IO) { storeAppFunctions.requestStockChange(productId, variationId, newQuantity) }

    /**
     * Opens an order in the WooCommerce app. Use it when the merchant wants to see the order in the app, or to do
     * something these functions can't, such as changing the order status, refunding it or contacting the customer.
     *
     * @param orderId The order ID, as returned by findOrders.
     * @return The order's screen in the WooCommerce app. Open it to take the merchant there.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = false)
    suspend fun openOrderInApp(orderId: Long): PendingIntent =
        withContext(Dispatchers.IO) { storeAppFunctions.openOrderInApp(orderId) }
}
