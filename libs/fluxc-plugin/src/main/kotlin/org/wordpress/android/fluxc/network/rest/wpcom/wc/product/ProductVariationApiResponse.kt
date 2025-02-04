package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.utils.getString

@Suppress("PropertyName", "VariableNaming")
class ProductVariationApiResponse : Response {
    var id: Long = 0L

    var description: String? = null
    var permalink: String? = null
    var sku: String? = null
    var global_unique_id: String? = null
    var status: String? = null
    var price: String? = null
    var regular_price: String? = null
    var sale_price: String? = null

    var date_on_sale_from: String? = null
    var date_on_sale_to: String? = null
    var date_on_sale_from_gmt: String? = null
    var date_on_sale_to_gmt: String? = null

    var tax_status: String? = null
    var tax_class: String? = null

    var date_created: String? = null
    var date_modified: String? = null

    var on_sale = false
    var purchasable = false
    var virtual = false
    var downloadable = false

    var backorders: String? = null
    var backorders_allowed = false
    var backordered = false

    var shipping_class: String? = null
    var shipping_class_id = 0

    var download_limit = 0L
    var download_expiry = 0

    var manage_stock = false
    var stock_quantity = 0.0
    var stock_status: String? = null

    var image: JsonElement? = null

    var weight: String? = null
    val menu_order: Int = 0

    val min_quantity: String? = null
    val max_quantity: String? = null
    val group_of_quantity: String? = null
    val variation_quantity_rules: String? = null

    var dimensions: JsonElement? = null
    var attributes: JsonElement? = null
    var downloads: JsonElement? = null
    var meta_data: JsonElement? = null

    @Suppress("ComplexMethod")
    fun asProductVariationModel() =
        WCProductVariationModel().apply {
            val response = this@ProductVariationApiResponse
            remoteVariationId = response.id
            permalink = response.permalink ?: ""

            dateCreated = response.date_created ?: ""
            dateModified = response.date_modified ?: ""

            status = response.status ?: ""
            description = response.description ?: ""
            sku = response.sku ?: ""
            globalUniqueId = response.global_unique_id ?: ""

            price = response.price ?: ""
            regularPrice = response.regular_price ?: ""
            salePrice = response.sale_price ?: ""
            onSale = response.on_sale

            dateOnSaleFrom = response.date_on_sale_from ?: ""
            dateOnSaleTo = response.date_on_sale_to ?: ""
            dateOnSaleFromGmt = response.date_on_sale_from_gmt ?: ""
            dateOnSaleToGmt = response.date_on_sale_to_gmt ?: ""

            taxStatus = response.tax_status ?: ""
            taxClass = response.tax_class ?: ""

            backorders = response.backorders ?: ""
            backordersAllowed = response.backorders_allowed
            backordered = response.backordered

            shippingClass = response.shipping_class ?: ""
            shippingClassId = response.shipping_class_id

            downloadLimit = response.download_limit
            downloadExpiry = response.download_expiry

            virtual = response.virtual
            downloadable = response.downloadable
            purchasable = response.purchasable

            manageStock = response.manage_stock
            stockQuantity = response.stock_quantity
            stockStatus = response.stock_status ?: ""

            attributes = response.attributes?.toString() ?: ""

            weight = response.weight ?: ""
            menuOrder = response.menu_order

            attributes = response.attributes?.toString() ?: ""
            downloads = response.downloads?.toString() ?: ""

            response.dimensions?.asJsonObject?.let { json ->
                length = json.getString("length") ?: ""
                width = json.getString("width") ?: ""
                height = json.getString("height") ?: ""
            }

            image = response.image?.toString() ?: ""

            minAllowedQuantity = response.min_quantity?.toInt() ?: -1
            maxAllowedQuantity = response.max_quantity?.let {
                if (it.isEmpty()) "0" else it
            }?.toInt() ?: -1
            groupOfQuantity = response.group_of_quantity?.toInt() ?: -1
            overrideProductQuantities = response.variation_quantity_rules?.let {
                it == "yes"
            } ?: false

            metadata = response.meta_data?.toString()
        }
}
