package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.variations.getNameForPOS
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCOrderStore
import java.util.Date
import javax.inject.Inject

class WooPosTotalsRepository @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val dateUtils: DateUtils,
    private val getProductById: WooPosGetProductById,
    private val getVariationById: WooPosGetVariationById,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
    private val resourceProvider: ResourceProvider,
) {
    private var orderCreationJob: Deferred<Result<Order>>? = null

    suspend fun createOrderFromCartItems(
        itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>
    ): Result<Order> {
        check(itemClickedDataList.map { it.id }.isNotEmpty()) { "List of IDs is empty" }

        orderCreationJob?.cancel()

        return withContext(IO) {
            check(itemClickedDataList.all { it.id >= 0 }) { "Invalid item ID" }
            orderCreationJob = async {
                val order = createOrder(itemClickedDataList)
                orderCreateEditRepository.createOrUpdateOrder(order)
            }
            orderCreationJob!!.await()
        }
    }

    private suspend fun createOrder(itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData>): Order {
        val products = itemClickedDataList.filterIsInstance<WooPosItemsViewModel.ItemClickedData.Product>()
        val coupons = itemClickedDataList.filterIsInstance<WooPosItemsViewModel.ItemClickedData.Coupon>()
        return Order.getEmptyOrder(
            dateCreated = dateUtils.getCurrentDateInSiteTimeZone() ?: Date(),
            dateModified = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        ).copy(
            status = Order.Status.Custom(Order.Status.AUTO_DRAFT),
            items = createProductItems(products),
            couponLines = createCouponLines(coupons),
        )
    }

    private suspend fun createProductItems(
        itemClickedDataList: List<WooPosItemsViewModel.ItemClickedData.Product>
    ): List<Order.Item> {
        return itemClickedDataList
            .groupingBy { it.id }
            .eachCount()
            .map { (id, quantity) ->
                val itemData = itemClickedDataList.find { it.id == id }!!
                when (itemData) {
                    is WooPosItemsViewModel.ItemClickedData.Product.Simple -> createSimpleProductOrderItem(
                        quantity,
                        itemData
                    )
                    is WooPosItemsViewModel.ItemClickedData.Product.Variation -> createVariationOrderItem(
                        quantity,
                        itemData
                    )
                }
            }
    }

    private fun createCouponLines(coupons: List<WooPosItemsViewModel.ItemClickedData.Coupon>): List<Order.CouponLine> {
        return coupons.map {
            Order.CouponLine(
                code = it.couponCode,
                id = it.id,
            )
        }
    }

    private suspend fun createSimpleProductOrderItem(
        quantity: Int,
        itemData: WooPosItemsViewModel.ItemClickedData.Product.Simple
    ): Order.Item {
        val productResult = getProductById(itemData.id)!!
        return Order.Item.EMPTY.copy(
            itemId = 0L,
            productId = itemData.id,
            variationId = 0L,
            quantity = quantity.toFloat(),
            total = EMPTY_TOTALS_SUBTOTAL_VALUE,
            subtotal = EMPTY_TOTALS_SUBTOTAL_VALUE,
            attributesList = emptyList(),
            name = productResult.name,
        )
    }

    private suspend fun createVariationOrderItem(
        quantity: Int,
        itemData: WooPosItemsViewModel.ItemClickedData.Product.Variation
    ): Order.Item {
        val productResult = getProductById(itemData.productId)!!
        val variationResult = getVariationById(
            productId = itemData.productId,
            variationId = itemData.id
        )!!
        variationResult.getNameForPOS(productResult, resourceProvider)
        return Order.Item.EMPTY.copy(
            itemId = 0L,
            productId = itemData.productId,
            variationId = variationResult.remoteVariationId,
            quantity = quantity.toFloat(),
            total = EMPTY_TOTALS_SUBTOTAL_VALUE,
            subtotal = EMPTY_TOTALS_SUBTOTAL_VALUE,
            attributesList = variationResult.attributes
                .filterNot { it.name.isNullOrEmpty() || it.option.isNullOrEmpty() }
                .map { Order.Item.Attribute(it.name!!, it.option!!) },
            name = variationResult.getName(productResult),
        )
    }

    suspend fun getOrderById(orderId: Long) = withContext(IO) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let {
            orderMapper.toAppModel(it)
        }
    }

    private companion object {
        /**
         * This magic value used to indicate that we don't want to send subtotals and totals
         * And let the backend to calculate them.
         */
        val EMPTY_TOTALS_SUBTOTAL_VALUE = -Double.MAX_VALUE.toBigDecimal()
    }
}
