package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import java.math.BigDecimal

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
) {
    enum class Option {
        DEFAULT,
        SIGNATURE,
        ADULT_SIGNATURE
    }
}

data class WooShippingRateOptionsModel(
    val rateOptions: Map<Option, WooShippingRateModel>
) {
    val defaultRate: WooShippingRateModel
        get() = rateOptions[Option.DEFAULT] ?: rateOptions.values.first()
}
