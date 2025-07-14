package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.ResourceProvider

fun Product.getStockText(context: Context): String {
    return getStockText(
        this.specialStockStatus ?: this.stockStatus,
        this.productType,
        this.stockQuantity,
        this.numVariations,
    ) { resId: Int, param: Any? ->
        param?.let {
            context.getString(resId, it)
        } ?: context.getString(resId)
    }
}

fun Product.getStockText(resourceProvider: ResourceProvider): String {
    return getStockText(
        this.specialStockStatus ?: this.stockStatus,
        this.productType,
        this.stockQuantity,
        this.numVariations,
    ) { resId: Int, param: Any? ->
        param?.let {
            resourceProvider.getString(resId, it)
        } ?: resourceProvider.getString(resId)
    }
}

fun OrderCreationProduct.getStockText(context: Context): String {
    return getStockText(
        this.productInfo.stockStatus,
        null,
        this.productInfo.stockQuantity
    ) { resId: Int, param: Any? ->
        param?.let {
            context.getString(resId, it)
        } ?: context.getString(resId)
    }
}

private fun getStockText(
    stockStatus: ProductStockStatus,
    productType: ProductType?,
    stockQuantity: Double = 0.0,
    numVariations: Int = 0,
    getString: (resId: Int, param: Any?) -> String
): String {
    return when (stockStatus) {
        ProductStockStatus.InStock -> getInStockText(productType, stockQuantity, numVariations, getString)
        ProductStockStatus.OutOfStock -> getString(R.string.product_stock_status_out_of_stock, null)
        ProductStockStatus.OnBackorder -> getString(R.string.product_stock_status_on_backorder, null)
        ProductStockStatus.InsufficientStock -> getString(R.string.product_stock_status_insufficient_stock, null)
        else -> stockStatus.value
    }
}

private fun getInStockText(
    productType: ProductType?,
    stockQuantity: Double = 0.0,
    numVariations: Int = 0,
    getString: (resId: Int, param: Any?) -> String
): String {
    return when {
        productType == ProductType.VARIABLE && numVariations > 0 -> {
            getString(
                R.string.product_stock_status_instock_with_variations,
                numVariations
            )
        }

        stockQuantity > 0 -> {
            getString(
                R.string.product_stock_count,
                StringUtils.formatCountDecimal(stockQuantity)
            )
        }

        else -> getString(R.string.product_stock_status_instock, null)
    }
}

fun OrderCreationProduct.getVariationAttributesAndStockText(context: Context): String {
    return buildString {
        append(item.attributesDescription.takeIf { it.isNotEmpty() }?.let { "$it \u2981 " } ?: StringUtils.EMPTY)
        append(getStockText(context))
    }
}
