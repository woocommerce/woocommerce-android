package org.wordpress.android.fluxc.model.shippinglabels

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import java.math.BigDecimal

@Table(addOn = WellSqlConfig.ADDON_WOOCOMMERCE)
class WCShippingLabelModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId = 0
    @Column var remoteOrderId = 0L // The remote identifier for the parent order object
    @Column var remoteShippingLabelId = 0L // The unique identifier for this note on the server
    @Column var trackingNumber = ""
    @Column var carrierId = ""
    @Column var dateCreated: Long? = null
    @Column var expiryDate: Long? = null
    @Column var serviceName = ""
    @Column var status = ""
    @Column var packageName = ""
    @Column var rate = 0F
    @Column var refundableAmount = 0F
    @Column var currency = ""
    @Column var productNames = "" // list of product names the shipping label was purchased for
    @Column var productIds = "" // list of product ids the shipping label was purchased for
    @Column var formData = "" // map containing package and product details related to that shipping label
    @Column var refund = "" // map containing refund information for a shipping label
    @Column var commercialInvoiceUrl: String? = null // URL pointing to the international commercial URL

    override fun getId() = id

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        private val gson by lazy { Gson() }
    }

    /**
     * Returns the destination details wrapped in a [ShippingLabelAddress].
     */
    fun getDestinationAddress() = getFormData()?.destination

    /**
     * Returns the shipping details wrapped in a [ShippingLabelAddress].
     */
    fun getOriginAddress() = getFormData()?.origin

    /**
     * Returns the product details for the order wrapped in a list of [ProductItem]
     */
    fun getProductItems() = getFormData()?.selectedPackage?.defaultBox?.productItems ?: emptyList()

    /**
     * Returns default data related to the order such as the origin address,
     * destination address and product items associated with the order.
     */
    private fun getFormData(): FormData? {
        val responseType = object : TypeToken<FormData>() {}.type
        return gson.fromJson(formData, responseType) as? FormData
    }

    /**
     * Returns the list of products the shipping labels were purchased for
     *
     * For instance: "[Belt, Cap, Herman Miller Chair Embody]" would be split into a list
     * ["Belt", "Cap", "Herman Miller Chair Embody"]
     */
    fun getProductNameList(): List<String> {
        return productNames
                .trim() // remove extra spaces between commas
                .removePrefix("[") // remove the String prefix
                .removeSuffix("]") // remove the String suffix
                .split(",") // split the string into list using comma spearators
    }

    /**
     * Returns the list of products the shipping labels were purchased for
     *
     * For instance: "[60, 61, 62]" would be split into a list
     * [60, 61, 62]
     */
    fun getProductIdsList(): List<Long> {
        return productIds
                .trim() // remove extra spaces between the brackets
                .removePrefix("[") // remove the String prefix
                .removeSuffix("]") // remove the String suffix
                .split(",") // split the string into list using comma separators
                .filter { it.isNotEmpty() }
                .map { it.trim().toLong() }
    }

    /**
     * Returns data related to the refund of a shipping label.
     * Will only be available in the API if a refund has been initiated
     */
    fun getRefundModel(): WCShippingLabelRefundModel? {
        val responseType = object : TypeToken<WCShippingLabelRefundModel>() {}.type
        return gson.fromJson(refund, responseType) as? WCShippingLabelRefundModel
    }

    /**
     * Model class corresponding to the [formData] map from the API response.
     * The [formData] contains the [origin] and [destination] address and the
     * product details associated with the order.
     * (nested under [selectedPackage] -> [DefaultBox] -> List of [ProductItem]).
     */
    class FormData(
        val origin: ShippingLabelAddress? = null,
        val destination: ShippingLabelAddress? = null,
        @SerializedName("selected_packages") val selectedPackage: SelectedPackage? = null
    )

    data class ShippingLabelAddress(
        val company: String? = null,
        val name: String? = null,
        val phone: String? = null,
        val country: String? = null,
        val state: String? = null,
        val address: String? = null,
        @SerializedName("address_2") val address2: String? = null,
        val city: String? = null,
        val postcode: String? = null
    ) {
        enum class Type {
            ORIGIN,
            DESTINATION
        }
    }

    data class ShippingLabelPackage(
        val id: String,
        @SerializedName("box_id") val boxId: String,
        val height: Float,
        val length: Float,
        val width: Float,
        val weight: Float,
        @SerializedName("is_letter") val isLetter: Boolean = false,
        val hazmat: HazmatCategory? = null
    )

    class SelectedPackage {
        @SerializedName("default_box") val defaultBox: DefaultBox? = null
    }

    class DefaultBox {
        @SerializedName("items") val productItems: List<ProductItem>? = null
    }

    class ProductItem {
        val height: BigDecimal? = null
        val length: BigDecimal? = null
        val quantity: Int? = null
        val width: BigDecimal? = null
        val name: String? = null
        val url: String? = null
        val value: BigDecimal? = null
        @SerializedName("product_id") val productId: Long? = null
    }

    class WCShippingLabelRefundModel {
        val status: String? = null
        @SerializedName("request_date") val requestDate: Long? = null
    }
    enum class HazmatCategory {
        PRIMARY_CONTAINED,
        PRIMARY_PACKED,
        PRIMARY,
        SECONDARY_CONTAINED,
        SECONDARY_PACKED,
        SECONDARY,
        ORMD,
        LITHIUM,
        LIMITED_QUANTITY,
        AIR_ELIGIBLE_ETHANOL,
        CLASS_1,
        CLASS_3,
        CLASS_4,
        CLASS_5,
        CLASS_6,
        CLASS_7,
        CLASS_8_CORROSIVE,
        CLASS_8_WET_BATTERY,
        CLASS_9_NEW_LITHIUM_INDIVIDUAL,
        CLASS_9_USED_LITHIUM,
        CLASS_9_NEW_LITHIUM_DEVICE,
        CLASS_9_DRY_ICE,
        CLASS_9_UNMARKED_LITHIUM,
        CLASS_9_MAGNETIZED,
        DIVISION_4_1,
        DIVISION_5_1,
        DIVISION_5_2,
        DIVISION_6_1,
        DIVISION_6_2,
        EXCEPTED_QUANTITY_PROVISION,
        GROUND_ONLY,
        ID8000,
        LIGHTERS,
        SMALL_QUANTITY_PROVISION
    }
}
