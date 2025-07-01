package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import android.os.Parcelable
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class WooShippingRateModel(
    val packageId: String,
    val shipmentId: String,
    val rateId: String,
    val serviceId: String,
    val carrierId: String,
    val serviceName: String,
    val deliveryDays: Int,
    val price: BigDecimal,
    val isTrackingEnabled: Boolean,
    val hasFreePickup: Boolean,
    val insurance: BigDecimal?,
    val deliveryDate: String?,
    val isDeliveryDateGuaranteed: Boolean,
    val isSelected: Boolean,
    val listRate: BigDecimal,
    val retailRate: BigDecimal,
    val option: Option,
    val carrier: WooShippingCarrier
) : Parcelable {
    enum class Option(
        val id: String,
        val typeId: String = id,
        /**
         * Indicates whether this option is an additional service that can be added to a base shipping rate.
         */
        val isAdditionalOption: Boolean
    ) {
        DEFAULT(id = "default", isAdditionalOption = false),
        SIGNATURE(id = "signature_required", typeId = "signature", isAdditionalOption = false),
        ADULT_SIGNATURE(id = "adult_signature_required", typeId = "signature", isAdditionalOption = false),
        CARBON_NEUTRAL(id = "carbon_neutral", isAdditionalOption = true),
        ADDITIONAL_HANDLING(id = "additional_handling", isAdditionalOption = true),
        SATURDAY_DELIVERY(id = "saturday_delivery", isAdditionalOption = true);

        companion object {
            fun fromId(id: String): Option? {
                return entries.firstOrNull { it.id == id }
            }
        }
    }
}

data class WooShippingRateOptionsModel(
    val rateOptions: Map<Option, WooShippingRateModel>
) {
    val defaultRate: WooShippingRateModel
        get() = rateOptions[Option.DEFAULT] ?: rateOptions.values.first()
}

data class WooShippingSelectedRateModel(
    val rate: WooShippingRateModel,
    val parentRate: WooShippingRateModel?,
    val additionalRates: List<WooShippingRateModel>
) {
    private val defaultRate: WooShippingRateModel
        get() = parentRate ?: rate

    fun getSurcharge(option: Option): BigDecimal {
        return if (option.isAdditionalOption) {
            additionalRates.first { it.option == option }.price - defaultRate.price
        } else {
            require(option == rate.option)
            rate.price - defaultRate.price
        }
    }
}
