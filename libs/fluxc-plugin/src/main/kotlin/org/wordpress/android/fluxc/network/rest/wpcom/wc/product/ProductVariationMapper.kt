package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.util.AppLog

object ProductVariationMapper {
    /**
     * Build json body of product items to be updated to the backend.
     *
     * This method checks if there is a cached version of the product stored locally.
     * If not, it generates a new product model for the same product ID, with default fields
     * and verifies that the [updatedVariationModel] has fields that are different from the default
     * fields of [variationModel]. This is to ensure that we do not update product fields that do not contain any
     * changes.
     */
    @Suppress(
        "ForbiddenComment",
        "LongMethod",
        "ComplexMethod",
        "SwallowedException",
        "TooGenericExceptionCaught"
    )
    fun variantModelToProductJsonBody(
        variationModel: WCProductVariationModel?,
        updatedVariationModel: WCProductVariationModel
    ): HashMap<String, Any> {
        val body = HashMap<String, Any>()

        val storedVariationModel = variationModel ?: WCProductVariationModel().apply {
            remoteProductId = updatedVariationModel.remoteProductId
            remoteVariationId = updatedVariationModel.remoteVariationId
        }
        if (storedVariationModel.description != updatedVariationModel.description) {
            body["description"] = updatedVariationModel.description
        }
        if (storedVariationModel.sku != updatedVariationModel.sku) {
            body["sku"] = updatedVariationModel.sku
        }
        if (storedVariationModel.globalUniqueId != updatedVariationModel.globalUniqueId) {
            body["global_unique_id"] = updatedVariationModel.globalUniqueId
        }
        if (storedVariationModel.status != updatedVariationModel.status) {
            body["status"] = updatedVariationModel.status
        }
        if (storedVariationModel.manageStock != updatedVariationModel.manageStock) {
            body["manage_stock"] = updatedVariationModel.manageStock
        }

        // only allowed to change the following params if manageStock is enabled
        if (updatedVariationModel.manageStock) {
            if (storedVariationModel.stockQuantity != updatedVariationModel.stockQuantity) {
                // Conversion/rounding down because core API only accepts Int value for stock quantity.
                // On the app side, make sure it only allows whole decimal quantity when updating, so that
                // there's no undesirable conversion effect.
                body["stock_quantity"] = updatedVariationModel.stockQuantity.toInt()
            }
            if (storedVariationModel.backorders != updatedVariationModel.backorders) {
                body["backorders"] = updatedVariationModel.backorders
            }
        }
        if (storedVariationModel.stockStatus != updatedVariationModel.stockStatus) {
            body["stock_status"] = updatedVariationModel.stockStatus
        }
        if (storedVariationModel.regularPrice != updatedVariationModel.regularPrice) {
            body["regular_price"] = updatedVariationModel.regularPrice
        }
        if (storedVariationModel.salePrice != updatedVariationModel.salePrice) {
            body["sale_price"] = updatedVariationModel.salePrice
        }
        if (storedVariationModel.dateOnSaleFromGmt != updatedVariationModel.dateOnSaleFromGmt) {
            body["date_on_sale_from_gmt"] = updatedVariationModel.dateOnSaleFromGmt
        }
        if (storedVariationModel.dateOnSaleToGmt != updatedVariationModel.dateOnSaleToGmt) {
            body["date_on_sale_to_gmt"] = updatedVariationModel.dateOnSaleToGmt
        }
        if (storedVariationModel.taxStatus != updatedVariationModel.taxStatus) {
            body["tax_status"] = updatedVariationModel.taxStatus
        }
        if (storedVariationModel.taxClass != updatedVariationModel.taxClass) {
            body["tax_class"] = updatedVariationModel.taxClass
        }
        if (storedVariationModel.weight != updatedVariationModel.weight) {
            body["weight"] = updatedVariationModel.weight
        }

        val dimensionsBody = mutableMapOf<String, String>()
        if (storedVariationModel.height != updatedVariationModel.height) {
            dimensionsBody["height"] = updatedVariationModel.height
        }
        if (storedVariationModel.width != updatedVariationModel.width) {
            dimensionsBody["width"] = updatedVariationModel.width
        }
        if (storedVariationModel.length != updatedVariationModel.length) {
            dimensionsBody["length"] = updatedVariationModel.length
        }
        if (dimensionsBody.isNotEmpty()) {
            body["dimensions"] = dimensionsBody
        }
        if (storedVariationModel.shippingClass != updatedVariationModel.shippingClass) {
            body["shipping_class"] = updatedVariationModel.shippingClass
        }
        // TODO: Once removal is supported, we can remove the extra isNotBlank() condition
        if (storedVariationModel.image != updatedVariationModel.image && updatedVariationModel.image.isNotBlank()) {
            body["image"] = updatedVariationModel.getImageModel()?.toJson() ?: ""
        }
        if (storedVariationModel.menuOrder != updatedVariationModel.menuOrder) {
            body["menu_order"] = updatedVariationModel.menuOrder
        }

        if (storedVariationModel.minAllowedQuantity != updatedVariationModel.minAllowedQuantity) {
            body["min_quantity"] = updatedVariationModel.minAllowedQuantity
        }

        if (storedVariationModel.maxAllowedQuantity != updatedVariationModel.maxAllowedQuantity) {
            body["max_quantity"] = updatedVariationModel.maxAllowedQuantity
        }

        if (storedVariationModel.groupOfQuantity != updatedVariationModel.groupOfQuantity) {
            body["group_of_quantity"] = updatedVariationModel.groupOfQuantity
        }

        if (storedVariationModel.attributes != updatedVariationModel.attributes) {
            JsonParser().apply {
                body["attributes"] = try {
                    parse(updatedVariationModel.attributes).asJsonArray
                } catch (ex: Exception) {
                    JsonArray()
                }
            }
        }
        if (storedVariationModel.metadata != updatedVariationModel.metadata) {
            JsonParser().apply {
                body["meta_data"] = try {
                    parse(updatedVariationModel.metadata).asJsonArray
                } catch (ex: Exception) {
                    AppLog.e(AppLog.T.API, "Error parsing product variation metadata", ex)
                    JsonArray()
                }
            }
        }

        return body
    }
}
