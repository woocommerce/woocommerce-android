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
    val discount: BigDecimal,
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
        /**
         * Indicates whether this option is an additional service that can be added to a base shipping rate.
         */
        val isAdditionalOption: Boolean
    ) {
        DEFAULT(isAdditionalOption = false),
        SIGNATURE(isAdditionalOption = false),
        ADULT_SIGNATURE(isAdditionalOption = false),
        CARBON_NEUTRAL(isAdditionalOption = true),
        ADDITIONAL_HANDLING(isAdditionalOption = true),
        SATURDAY_DELIVERY(isAdditionalOption = true)
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
)
