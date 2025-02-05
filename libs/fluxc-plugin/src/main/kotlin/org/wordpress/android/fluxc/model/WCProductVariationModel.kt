package org.wordpress.android.fluxc.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.fluxc.network.utils.getLong
import org.wordpress.android.fluxc.network.utils.getString
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.lang.IllegalStateException

typealias VariationAttributes = List<ProductVariantOption>

/**
 * Product variations - see http://woocommerce.github.io/woocommerce-rest-api-docs/#product-variations
 * As with WCProductModel, the backend returns more properties than are supported below
 */
@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
data class WCProductVariationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteProductId = 0L
    @Column var remoteVariationId = 0L

    @Column var dateCreated = ""
    @Column var dateModified = ""

    @Column var description = ""
    @Column var permalink = ""
    @Column var sku = ""
    @Column var globalUniqueId = ""
    @Column var status = ""

    @Column var price = ""
    @Column var regularPrice = ""
    @Column var salePrice = ""

    @Column var dateOnSaleFrom = ""
    @Column var dateOnSaleTo = ""
    @Column var dateOnSaleFromGmt = ""
    @Column var dateOnSaleToGmt = ""

    @Column var taxStatus = "" // taxable, shipping, none
    @Column var taxClass = ""

    @Column var onSale = false
    @Column var purchasable = false
    @Column var virtual = false
    @Column var downloadable = false

    @Column var downloadLimit = 0L
    @Column var downloadExpiry = 0

    @Column var downloads = "" // array of downloadable files
    @Column var backorders = "" // no, notify, yes

    @Column var backordersAllowed = false
    @Column var backordered = false

    @Column var shippingClass = ""
    @Column var shippingClassId = 0

    @Column var manageStock = false
    @Column var stockQuantity = 0.0
    @Column var stockStatus = ""

    @Column var image = ""

    @Column var weight = ""
    @Column var length = ""
    @Column var width = ""
    @Column var height = ""

    @Column var minAllowedQuantity = -1
    @Column var maxAllowedQuantity = -1
    @Column var groupOfQuantity = -1
    @Column var overrideProductQuantities = false

    @Column var menuOrder = 0

    @Column var attributes = ""

    @Column var metadata: String? = null

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    data class ProductVariantOption(
        val id: Long? = null,
        val name: String? = null,
        val option: String? = null
    )

    private val gson by lazy { Gson() }

    val attributeList
        get() = gson.fromJson(attributes, Array<ProductVariantOption>::class.java)

    fun addVariant(newAttribute: ProductVariantOption) =
            mutableListOf<ProductVariantOption>()
                    .apply {
                        attributeList
                                ?.takeIf { it.isNotEmpty() }
                                ?.let { addAll(it) }
                        add(newAttribute)
                    }.also { attributes = gson.toJson(it) }

    fun removeVariant(removableAttribute: ProductVariantOption) =
            mutableListOf<ProductVariantOption>().apply {
                attributeList
                        ?.takeIf { it.isNotEmpty() }
                        ?.filter { removableAttribute.id != it.id }
                        ?.let { addAll(it) }
            }.also { attributes = gson.toJson(it) }

    /**
     * Parses the images json array into a list of product images
     */
    @Suppress("ReturnCount")
    fun getImageModel(): WCProductImageModel? {
        if (image.isNotBlank()) {
            try {
                with(gson.fromJson(image, JsonElement::class.java).asJsonObject) {
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

    /**
     * Deserializes the JSON contained in [attributes] into a list of [ProductVariantOption] objects.
     */
    fun getProductVariantOptions(): List<ProductVariantOption> {
        val responseType = object : TypeToken<List<ProductVariantOption>>() {}.type
        return gson.fromJson(attributes, responseType) as? List<ProductVariantOption> ?: emptyList()
    }
}
