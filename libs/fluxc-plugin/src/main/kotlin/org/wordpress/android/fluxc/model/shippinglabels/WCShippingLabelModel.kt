package org.wordpress.android.fluxc.model.shippinglabels

import androidx.room.Entity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.math.BigDecimal

@Entity(
    tableName = "ShippingLabelEntity",
    primaryKeys = ["localSiteId", "remoteOrderId", "remoteShippingLabelId"],
)
data class WCShippingLabelModel(
    val localSiteId: Int = 0,
    val remoteOrderId: Long = 0L, // The remote identifier for the parent order object
    val remoteShippingLabelId: Long = 0L, // The unique identifier for this note on the server
    val trackingNumber: String = "",
    val carrierId: String = "",
    val dateCreated: Long? = null,
    val expiryDate: Long? = null,
    val serviceName: String = "",
    val status: String = "",
    val packageName: String = "",
    val rate: Float = 0F,
    val refundableAmount: Float = 0F,
    val currency: String = "",
    val productNames: String = "", // list of product names the shipping label was purchased for
    val productIds: String = "", // list of product ids the shipping label was purchased for
    val formData: String = "", // map containing package and product details related to that shipping label
    val refund: String = "", // map containing refund information for a shipping label
    val commercialInvoiceUrl: String? = null // URL pointing to the international commercial URL
) {

    companion object {
        @Deprecated("Database entity should not keep a reference to Gson")
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
        PRIMARY,
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
