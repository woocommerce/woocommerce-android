package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.orders.creation.ProductUIModel
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.ResourceProvider

object ProductUtils {
    fun getStockText(product: Product, context: Context): String {
        return when (product.specialStockStatus ?: product.stockStatus) {
            ProductStockStatus.InStock -> {
                if (product.productType == ProductType.VARIABLE) {
                    if (product.numVariations > 0) {
                        context.getString(
                            R.string.product_stock_status_instock_with_variations,
                            product.numVariations
                        )
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
                } else {
                    if (product.stockQuantity > 0) {
                        context.getString(
                            R.string.product_stock_count,
                            StringUtils.formatCountDecimal(product.stockQuantity)
                        )
                    } else {
                        context.getString(R.string.product_stock_status_instock)
                    }
                }
            }
            ProductStockStatus.OutOfStock -> {
                context.getString(R.string.product_stock_status_out_of_stock)
            }
            ProductStockStatus.OnBackorder -> {
                context.getString(R.string.product_stock_status_on_backorder)
            }
            ProductStockStatus.InsufficientStock -> {
                context.getString(R.string.product_stock_status_insufficient_stock)
            }
            else -> {
                product.stockStatus.value
            }
        }
    }

    fun getStockText(product: Product, resourceProvider: ResourceProvider): String {
        return when (product.specialStockStatus ?: product.stockStatus) {
            ProductStockStatus.InStock -> {
                if (product.productType == ProductType.VARIABLE) {
                    if (product.numVariations > 0) {
                        resourceProvider.getString(
                            R.string.product_stock_status_instock_with_variations,
                            product.numVariations
                        )
                    } else {
                        resourceProvider.getString(R.string.product_stock_status_instock)
                    }
                } else {
                    if (product.stockQuantity > 0) {
                        resourceProvider.getString(
                            R.string.product_stock_count,
                            StringUtils.formatCountDecimal(product.stockQuantity)
                        )
                    } else {
                        resourceProvider.getString(R.string.product_stock_status_instock)
                    }
                }
            }
            ProductStockStatus.OutOfStock -> {
                resourceProvider.getString(R.string.product_stock_status_out_of_stock)
            }
            ProductStockStatus.OnBackorder -> {
                resourceProvider.getString(R.string.product_stock_status_on_backorder)
            }
            ProductStockStatus.InsufficientStock -> {
                resourceProvider.getString(R.string.product_stock_status_insufficient_stock)
            }
            else -> {
                product.stockStatus.value
            }
        }
    }

    fun getStockText(product: ProductUIModel, context: Context): String {
        return when (product.stockStatus) {
            ProductStockStatus.InStock -> {
                if (product.stockQuantity > 0) {
                    context.getString(
                        R.string.product_stock_count,
                        StringUtils.formatCountDecimal(product.stockQuantity)
                    )
                } else {
                    context.getString(R.string.product_stock_status_instock)
                }
            }
            ProductStockStatus.OutOfStock -> {
                context.getString(R.string.product_stock_status_out_of_stock)
            }
            ProductStockStatus.OnBackorder -> {
                context.getString(R.string.product_stock_status_on_backorder)
            }
            ProductStockStatus.InsufficientStock -> {
                context.getString(R.string.product_stock_status_insufficient_stock)
            }
            else -> {
                product.stockStatus.value
            }
        }
    }
}
