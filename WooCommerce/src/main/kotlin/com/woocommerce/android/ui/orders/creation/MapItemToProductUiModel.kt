package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

class MapItemToProductUiModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val variationDetailRepository: VariationDetailRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val currencyFormatter: CurrencyFormatter,
) {
    suspend operator fun invoke(item: Order.Item, currencySymbol: String? = null): ProductUIModel {
        val decimalFormatter = getDecimalFormatter(currencyFormatter, currencySymbol)
        return withContext(dispatchers.io) {
            if (item.isVariation) {
                val variation = variationDetailRepository.getVariation(item.productId, item.variationId)
                ProductUIModel(
                    item = item,
                    imageUrl = variation?.image?.source.orEmpty(),
                    isStockManaged = variation?.isStockManaged ?: false,
                    stockQuantity = variation?.stockQuantity ?: 0.0,
                    stockStatus = variation?.stockStatus ?: ProductStockStatus.InStock,
                    pricePreDiscount = decimalFormatter(item.pricePreDiscount),
                    priceTotal = decimalFormatter(item.total),
                    priceSubtotal = decimalFormatter(item.subtotal),
                    discountAmount = decimalFormatter(item.discount),
                    priceAfterDiscount = decimalFormatter(item.subtotal - item.discount)
                )
            } else {
                val product = productDetailRepository.getProduct(item.productId)
                ProductUIModel(
                    item = item,
                    imageUrl = product?.firstImageUrl.orEmpty(),
                    isStockManaged = product?.isStockManaged ?: false,
                    stockQuantity = product?.stockQuantity ?: 0.0,
                    stockStatus = product?.specialStockStatus ?: product?.stockStatus ?: ProductStockStatus.InStock,
                    pricePreDiscount = decimalFormatter(item.pricePreDiscount),
                    priceTotal = decimalFormatter(item.total),
                    priceSubtotal = decimalFormatter(item.subtotal),
                    discountAmount = decimalFormatter(item.discount),
                    priceAfterDiscount = decimalFormatter(item.subtotal - item.discount)
                )
            }
        }
    }
}

private fun getDecimalFormatter(
    currencyFormatter: CurrencyFormatter,
    currencyCode: String? = null
): (BigDecimal) -> String {
    return currencyCode?.let {
        currencyFormatter.buildBigDecimalFormatter(it)
    } ?: currencyFormatter.buildBigDecimalFormatter()
}
