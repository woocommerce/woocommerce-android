package org.wordpress.android.fluxc.model

import androidx.room.Entity
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

typealias VariationAttributes = List<ProductVariantOption>

/**
 * Product variations - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-variations
 * As with WCProductModel, the backend returns more properties than are supported below
 */
@Entity(
    tableName = "ProductVariationEntity",
    primaryKeys = ["localSiteId", "remoteProductId", "remoteVariationId"],
)
data class WCProductVariationModel(
    val localSiteId: LocalId = LocalId(0),
    val remoteProductId: RemoteId = RemoteId(0),
    val remoteVariationId: RemoteId = RemoteId(0),
    val dateCreated: String = "",
    val dateModified: String = "",
    val description: String = "",
    val permalink: String = "",
    val sku: String = "",
    val globalUniqueId: String = "",
    val status: String = "",
    val price: String = "",
    val regularPrice: String = "",
    val salePrice: String = "",
    val dateOnSaleFrom: String = "",
    val dateOnSaleTo: String = "",
    val dateOnSaleFromGmt: String = "",
    val dateOnSaleToGmt: String = "",
    val taxStatus: String = "", // taxable, shipping, none
    val taxClass: String = "",
    val onSale: Boolean = false,
    val purchasable: Boolean = false,
    val virtual: Boolean = false,
    val downloadable: Boolean = false,
    val downloadLimit: Long = 0L,
    val downloadExpiry: Int = 0,
    val downloads: String = "", // array of downloadable files
    val backorders: String = "", // no, notify, yes
    val backordersAllowed: Boolean = false,
    val backordered: Boolean = false,
    val shippingClass: String = "",
    val shippingClassId: Int = 0,
    val manageStock: Boolean = false,
    val stockQuantity: Double = 0.0,
    val stockStatus: String = "",
    val image: String = "",
    // The variation's own image as returned by edit-context fetches; blank when it has none.
    val editContextImage: String? = null,
    val weight: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val minAllowedQuantity: Int = -1,
    val maxAllowedQuantity: Int = -1,
    val groupOfQuantity: Int = -1,
    val overrideProductQuantities: Boolean = false,
    val menuOrder: Int = 0,
    val attributes: String = "",
    val metadata: String? = null,
) {

    data class ProductVariantOption(
        val id: Long? = null,
        val name: String? = null,
        val option: String? = null
    )

    private val gson by lazy { Gson() }

    val attributeList
        get() = gson.fromJson(attributes, Array<ProductVariantOption>::class.java)

    fun getImageModel(): WCProductImageModel? = parseImageModel(image)

    fun getEditContextImageModel(): WCProductImageModel? = editContextImage?.let { parseImageModel(it) }

    /**
     * Parses the image json into a product image
     */
    private fun parseImageModel(json: String): WCProductImageModel? {
        if (json.isNotBlank()) {
            try {
                with(gson.fromJson(json, JsonElement::class.java).asJsonObject) {
                    WCProductImageModel(this.getLong("id")).also {
                        it.name = this.getString("name") ?: ""
                        it.src = this.getString("src") ?: ""
                        it.alt = this.getString("alt") ?: ""
                        return it
                    }
                }
            } catch (e: JsonParseException) {
                AppLog.e(T.API, e)
                return null
            } catch (e: IllegalStateException) {
                AppLog.e(T.API, e)
                return null
            }
        }
        return null
    }
}
