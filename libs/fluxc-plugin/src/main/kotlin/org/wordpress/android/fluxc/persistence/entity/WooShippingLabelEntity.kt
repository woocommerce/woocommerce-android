package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.converters.ISO8601DateConverter
import org.wordpress.android.fluxc.utils.asStringOrNull
import java.math.BigDecimal
import java.util.Date

@Entity(
    primaryKeys = ["localSiteId", "orderId", "labelId"],
)
@TypeConverters(WooShippingLabelConverters::class, ISO8601DateConverter::class)
data class WooShippingLabelEntity(
    val localSiteId: LocalOrRemoteId.LocalId,
    val orderId: LocalOrRemoteId.RemoteId,
    val labelId: Long,
    val tracking: String,
    val refundableAmount: BigDecimal,
    val status: String,
    val created: Date?,
    val carrierId: String,
    val serviceName: String,
    val commercialInvoiceUrl: String?,
    val isCommercialInvoiceSubmittedElectronically: Boolean,
    val packageName: String,
    val isLetter: Boolean,
    val productNames: List<String>,
    val productIds: List<Long>?,
    val shipmentId: String?,
    val receiptItemId: Long,
    val createdDate: Date?,
    val mainReceiptId: Long,
    val rate: BigDecimal,
    val currency: String,
    val expiryDate: Long,
    val usedDate: Long?,
    val refund: Refund?,
    val hazmatCategory: String?,
    val originAddress: Address?,
    val destinationAddress: Address?
) {
    data class Refund(
        val status: String?,
        val requestDate: Date?
    )

    data class Address(
        val company: String?,
        val name: String?,
        val phone: String?,
        val countryCode: String?,
        val state: String?,
        val address1: String?,
        val address2: String?,
        val city: String?,
        val postcode: String?,
        val email: String?
    )
}

internal class WooShippingLabelConverters {
    @TypeConverter
    fun fromRefundEntity(refund: WooShippingLabelEntity.Refund?): String? {
        return refund?.let {
            JsonObject().apply {
                addProperty("status", it.status)
                addProperty("requestDate", it.requestDate?.time)
            }.toString()
        }
    }

    @TypeConverter
    fun toRefundEntity(value: String?): WooShippingLabelEntity.Refund? {
        return value?.let { json ->
            JsonParser.parseString(json).asJsonObject.let { jsonObject ->
                WooShippingLabelEntity.Refund(
                    status = jsonObject.get("status")?.asStringOrNull,
                    requestDate = jsonObject.get("requestDate")?.asLong?.let { Date(it) }
                )
            }
        }
    }

    @TypeConverter
    fun fromAddressEntity(address: WooShippingLabelEntity.Address?): String? {
        return address?.let {
            JsonObject().apply {
                addProperty("company", it.company)
                addProperty("name", it.name)
                addProperty("phone", it.phone)
                addProperty("countryCode", it.countryCode)
                addProperty("state", it.state)
                addProperty("address1", it.address1)
                addProperty("address2", it.address2)
                addProperty("city", it.city)
                addProperty("postcode", it.postcode)
                addProperty("email", it.email)
            }.toString()
        }
    }

    @TypeConverter
    fun toAddressEntity(value: String?): WooShippingLabelEntity.Address? {
        return value?.let { json ->
            JsonParser.parseString(json).asJsonObject.let { jsonObject ->
                WooShippingLabelEntity.Address(
                    company = jsonObject.get("company")?.asStringOrNull,
                    name = jsonObject.get("name")?.asStringOrNull,
                    phone = jsonObject.get("phone")?.asStringOrNull,
                    countryCode = jsonObject.get("countryCode")?.asStringOrNull,
                    state = jsonObject.get("state")?.asStringOrNull,
                    address1 = jsonObject.get("address1")?.asStringOrNull,
                    address2 = jsonObject.get("address2")?.asStringOrNull,
                    city = jsonObject.get("city")?.asStringOrNull,
                    postcode = jsonObject.get("postcode")?.asStringOrNull,
                    email = jsonObject.get("email")?.asStringOrNull
                )
            }
        }
    }
}
