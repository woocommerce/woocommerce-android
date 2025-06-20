package com.woocommerce.android.ui.orders.wooshippinglabels.networking

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.DestinationAddressDTO
import java.lang.reflect.Type
import java.math.BigDecimal

data class AccountSettingsDTO(
    val storeOptions: StoreOptionsDTO,
    val formData: FormDataDTO,
    val formMeta: FormMetaDTO
)

data class StoreOptionsDTO(
    @SerializedName("currency_symbol") val currencySymbol: String? = null,
    @SerializedName("dimension_unit") val dimensionUnit: String? = null,
    @SerializedName("weight_unit") val weightUnit: String? = null,
    @SerializedName("origin_country") val originCountry: String? = null
)

data class FormDataDTO(
    @SerializedName("selected_payment_method_id") val selectedPaymentId: Long?,
    @SerializedName("email_receipts") val emailReceipts: Boolean = false
)

data class FormMetaDTO(
    @SerializedName("payment_methods") val paymentMethods: List<PaymentMethodDTO>,
    @SerializedName("add_payment_method_url") val addPaymentMethodUrl: String,
    @SerializedName("can_manage_payments") val canManagePayments: Boolean = false,
    @SerializedName("can_edit_settings") val canEditSettings: Boolean = true,
    @SerializedName("master_user_name") val masterUserName: String = "",
    @SerializedName("master_user_wpcom_login") val masterUserWpcomLogin: String = ""
)

data class PaymentMethodDTO(
    @SerializedName("payment_method_id")
    val paymentMethodId: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("card_type")
    val cardType: String,
    @SerializedName("card_digits")
    val cardDigits: String,
    @SerializedName("expiry")
    val expiry: String
)

data class ConfigResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("config") val config: ConfigDTO
)

data class UpdateShipmentsResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data")
    @JsonAdapter(ShipmentMapDeserializer::class)
    val data: ShipmentMap
)

/**
 * Alias for a mapping of shipment id (as String) to the list of items contained in each shipment.
 */
typealias ShipmentMap = Map<String, List<Item>>

data class ConfigDTO(
    @SerializedName("shipments")
    @JsonAdapter(ShipmentMapDeserializer::class)
    val shipments: ShipmentMap? = null,
    @SerializedName("shippingLabelData") val shippingLabelData: ShippingLabelDataDTO,
)

data class ShippingLabelDataDTO(
    @SerializedName("currentOrderLabels") val currentOrderLabels: List<ShippingLabelDTO>
)

data class Item(@SerializedName("id") val id: Long?, @SerializedName("subItems") val subItems: List<String>?)

data class GetShippingLabelResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("labels") val shippingLabels: List<ShippingLabelDTO>? = null
)

data class GetShippingLabelStatusResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("label") val shippingLabel: ShippingLabelDTO? = null
)

data class ShippingLabelDTO(
    @SerializedName("label_id") val labelId: Long? = null,
    @SerializedName("tracking") val tracking: String? = null,
    @SerializedName("refundable_amount") val refundableAmount: BigDecimal? = null,
    @SerializedName("status") val status: ShippingLabelStatus = ShippingLabelStatus.UNKNOWN,
    @SerializedName("created") val created: Long? = null,
    @SerializedName("carrier_id") val carrierId: String? = null,
    @SerializedName("service_name") val serviceName: String? = null,
    @SerializedName("commercial_invoice_url") val commercialInvoiceUrl: String? = null,
    @SerializedName("is_commercial_invoice_submitted_electronically")
    val isCommercialInvoiceSubmittedElectronically: Boolean? = null,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("is_letter") val isLetter: Boolean? = null,
    @SerializedName("product_names") val productNames: List<String>? = null,
    @SerializedName("product_ids") val productIds: List<Long>? = null,
    @SerializedName("id") val shipmentId: String? = null,
    @SerializedName("receipt_item_id") val receiptItemId: Long? = null,
    @SerializedName("created_date") val createdDate: Long? = null,
    @SerializedName("main_receipt_id") val mainReceiptId: Long? = null,
    @SerializedName("rate") val rate: BigDecimal? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("expiry_date") val expiryDate: Long? = null,
    @SerializedName("refund") val refund: LabelRefund? = null
)

data class LabelRefund(@SerializedName("status") val status: String? = null)

data class PurchasedShippingLabelResponseDTO(
    val success: Boolean,
    val labels: List<ShippingLabelDTO>,
    @SerializedName("selected_rates") val selectedRates: Map<String, ShippingRatePurchaseDTO>,
    @SerializedName("selected_hazmat") val selectedHazmat: Map<String, HazmatDTO>,
    @SerializedName("selected_origin") val selectedOrigin: Map<String, OriginAddressPurchaseDTO>,
    @SerializedName("selected_destination") val selectedDestination: Map<String, DestinationAddressDTO>,
)

data class ShippingRatePurchaseDTO(val rate: ShippingRatePurchaseResponseDTO)

data class ShippingRatePurchaseResponseDTO(
    @SerializedName("rate_id") val rateId: String,
    @SerializedName("service_id") val serviceId: String,
    @SerializedName("carrier_id") val carrierId: String?,
    val title: String,
    val rate: BigDecimal,
    @SerializedName("retail_rate") val retailRate: BigDecimal? = null,
    @SerializedName("list_rate") val listRate: BigDecimal? = null,
    @SerializedName("is_selected") val isSelected: Boolean,
    val tracking: Boolean = false,
    val insurance: String?,
    @SerializedName("free_pickup") val freePickup: Boolean,
    @SerializedName("shipment_id") val shipmentId: String?,
    @SerializedName("delivery_days") val deliveryDays: Int,
    @SerializedName("delivery_date_guaranteed") val deliveryDateGuaranteed: Boolean,
    @SerializedName("delivery_date") val deliveryDate: String?
)

data class AddressDTO(
    val id: String? = null,
    @SerializedName("address_1") val address: String? = null,
    @SerializedName("address_2") val address2: String? = null,
    val city: String? = null,
    val company: String? = null,
    val country: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val postcode: String? = null,
    val state: String? = null,
    val email: String? = null,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("default_address") val defaultAddress: Boolean = false,
)

data class NormalizationResponseDTO(
    val success: Boolean,
    val normalizedAddress: AddressDTO,
    val address: AddressDTO,
    val isTrivialNormalization: Boolean
)

data class UpdateAddressResponseDTO(
    val success: Boolean,
    val address: AddressDTO,
    val isVerified: Boolean
)

data class VerifyDestinationAddressResponseDTO(
    val success: Boolean,
    val normalizedAddress: AddressDTO,
    val isVerified: Boolean
)

data class CustomsDTO(
    @SerializedName("contents_type") val contentsType: String,
    @SerializedName("contents_explanation") val contentExplanation: String,
    @SerializedName("restriction_type") val restrictionType: String,
    @SerializedName("restriction_comments") val restrictionComments: String,
    @SerializedName("non_delivery_option") val isReturnToSender: String,
    val itn: String,
    val items: List<CustomsItemDTO>
)

data class CustomsItemDTO(
    val description: String,
    val quantity: Float,
    val value: Double,
    val weight: Double,
    @SerializedName("hs_tariff_number") val hsTariffNumber: String,
    @SerializedName("origin_country") val originCountry: String,
    @SerializedName("product_id") val productId: Long
)

data class RefundLabelResponseDTO(val success: Boolean)

private class ShipmentMapDeserializer : JsonDeserializer<ShipmentMap> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ShipmentMap {
        // Handle string-encoded JSON or direct JSON object
        val jsonObject = if (json.isJsonPrimitive && json.asJsonPrimitive.isString) {
            JsonParser.parseString(json.asString).asJsonObject
        } else {
            json.asJsonObject
        }

        val mapType = object : TypeToken<ShipmentMap>() {}.type
        return Gson().fromJson(jsonObject, mapType)
    }
}
