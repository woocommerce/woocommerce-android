package com.woocommerce.android.ui.products

import com.google.gson.Gson
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel

object WcProductTestUtils {
    /**
     * Generates a single [WCProductSettingsModel] object for a mock Site
     */
    fun generateProductSettings(
        localSiteId: Int = 1,
        dimensionUnit: String = "in",
        weightUnit: String = "oz"
    ): WCProductSettingsModel {
        return WCProductSettingsModel(localSiteId).apply {
            this.dimensionUnit = dimensionUnit
            this.weightUnit = weightUnit
        }
    }

    /**
     * Generates a single [WCProductModel] object for a mock Site
     */
    fun generateProductDetail(
        productId: Int = 1,
        totalSales: Int = 0,
        productName: String = "WooTest",
        productType: ProductType = SIMPLE
    ): WCProductModel {
        return WCProductModel(productId).apply {
            this.name = productName
            this.type = productType.name
            this.totalSales = totalSales
            this.permalink =
                    "https://jamosova3.mystagingwebsite.com/product/stranger-things-t-shirt-product-add-on-text-field/"
            this.images = Gson().toJson(listOf(
                    mapOf("src" to "https://jamosova3.mystagingwebsite.com/wp-content/uploads/2018/11/eleven.jpg")))
            this.downloads = Gson().toJson(listOf(
                    mapOf("file" to "https://jamosova3.mystagingwebsite.com/wp-content/uploads/2018/11/eleven.jpg")))
        }
    }
}
