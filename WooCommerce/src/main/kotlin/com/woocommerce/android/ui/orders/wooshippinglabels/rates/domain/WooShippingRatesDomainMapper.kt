package com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOptionUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import java.math.BigDecimal
import javax.inject.Inject

class WooShippingRatesDomainMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter
) {
    operator fun invoke(
        rates: List<WooShippingRateOptionsModel>,
        currencyCode: String?
    ): Map<CarrierUI, List<ShippingRateUI>> {
        return rates.groupBy { it.defaultRate.carrier }.map { entry ->
            getCarrier(entry.key) to entry.value.map { getShippingRate(it, resourceProvider, currencyCode) }
        }.toMap()
    }

    private fun getCarrier(carrier: WooShippingCarrier): CarrierUI {
        return when (carrier) {
            WooShippingCarrier.FEDEX -> CarrierUI(
                carrier = carrier,
                name = "FEDEX",
                logoRes = R.drawable.fedex_logo
            )

            WooShippingCarrier.USPS -> CarrierUI(
                carrier = WooShippingCarrier.USPS,
                name = "USPS",
                logoRes = R.drawable.usps_logo
            )

            WooShippingCarrier.UPS -> CarrierUI(
                carrier = WooShippingCarrier.UPS,
                name = "UPS",
                logoRes = R.drawable.ups_logo
            )

            WooShippingCarrier.DHL -> CarrierUI(
                carrier = WooShippingCarrier.DHL,
                name = "DHL Express",
                logoRes = R.drawable.dhl_logo
            )

            WooShippingCarrier.UNKNOWN -> CarrierUI(
                carrier = WooShippingCarrier.UNKNOWN,
                name = "Unknown",
                logoRes = null
            )
        }
    }

    @Suppress("LongMethod")
    private fun getShippingRate(
        rate: WooShippingRateOptionsModel,
        resourceProvider: ResourceProvider,
        currencyCode: String?
    ): ShippingRateUI {
        val options = rate.rateOptions.mapValues {
            when (it.value.option) {
                Option.DEFAULT -> {
                    ShippingRateOptionUI(
                        title = it.value.serviceName,
                        formatedPrice = formatCurrency(it.value.price, currencyCode),
                        formattedFee = "",
                        feeDescription = "",
                        formattedOptionName = "",
                        formattedEstimatedDays = getEstimatedDays(it.value, resourceProvider),
                        shippingRateOptions = getShippingRateOptionsList(it.value, resourceProvider, currencyCode),
                        option = it.value.option,
                        rate = it.value
                    )
                }

                Option.SIGNATURE -> {
                    val fee = rate.rateOptions[Option.DEFAULT]?.let { default ->
                        it.value.price.minus(default.price)
                    }
                    val formattedFee = formatFee(fee, currencyCode)
                    ShippingRateOptionUI(
                        title = it.value.serviceName,
                        formatedPrice = formatCurrency(it.value.price, currencyCode),
                        formattedFee = formattedFee,
                        feeDescription = resourceProvider.getString(
                            R.string.shipping_label_rate_option_signature_required,
                            formattedFee
                        ),
                        formattedEstimatedDays = getEstimatedDays(it.value, resourceProvider),
                        shippingRateOptions = getShippingRateOptionsList(it.value, resourceProvider, currencyCode),
                        option = it.value.option,
                        rate = it.value,
                        formattedOptionName = resourceProvider.getString(
                            R.string.shipping_label_rate_option_signature_required_name
                        )
                    )
                }

                Option.ADULT_SIGNATURE -> {
                    val fee = rate.rateOptions[Option.DEFAULT]?.let { default ->
                        it.value.price.minus(default.price)
                    }
                    val formattedFee = formatFee(fee, currencyCode)
                    ShippingRateOptionUI(
                        title = it.value.serviceName,
                        formatedPrice = formatCurrency(it.value.price, currencyCode),
                        formattedFee = formattedFee,
                        feeDescription = resourceProvider.getString(
                            R.string.shipping_label_rate_option_adult_signature_required,
                            formattedFee
                        ),
                        formattedEstimatedDays = getEstimatedDays(it.value, resourceProvider),
                        shippingRateOptions = getShippingRateOptionsList(it.value, resourceProvider, currencyCode),
                        option = it.value.option,
                        rate = it.value,
                        formattedOptionName = resourceProvider.getString(
                            R.string.shipping_label_rate_option_adult_signature_required_name
                        )
                    )
                }
            }
        }

        val selectedOption = options[Option.DEFAULT] ?: options.values.first()

        return ShippingRateUI(
            options = options,
            selectedOption = selectedOption
        )
    }

    private fun getShippingRateOptionsList(
        rate: WooShippingRateModel,
        resourceProvider: ResourceProvider,
        currencyCode: String?
    ): List<String> {
        val options = mutableListOf<String>()
        if (rate.isTrackingEnabled) {
            val tracking = resourceProvider.getString(
                R.string.shipping_label_rate_included_options_tracking
            )
            options.add(tracking)
        }
        if (rate.insurance != null) {
            val insurance = resourceProvider.getString(
                R.string.shipping_label_rate_included_options_insurance,
                resourceProvider.getString(
                    R.string.shipping_label_rate_insurance_up_to,
                    formatCurrency(rate.insurance, currencyCode)
                )
            )
            options.add(insurance)
        }
        if (rate.hasFreePickup) {
            val freePickup = resourceProvider.getString(
                R.string.shipping_label_rate_included_options_free_pickup
            )
            options.add(freePickup)
        }
        return options
    }

    private fun getEstimatedDays(
        rate: WooShippingRateModel,
        resourceProvider: ResourceProvider
    ): String {
        return resourceProvider.getQuantityString(
            quantity = rate.deliveryDays,
            default = R.string.shipping_label_shipping_carrier_rates_delivery_estimate_many,
            one = R.string.shipping_label_shipping_carrier_rates_delivery_estimate_one
        )
    }

    private fun formatFee(price: BigDecimal?, currencyCode: String?): String {
        return when {
            price == null -> "N/A"
            price.isEqualTo(BigDecimal.ZERO) -> resourceProvider.getString(R.string.free)
            else -> formatCurrency(price, currencyCode)
        }
    }

    private fun formatCurrency(amount: BigDecimal, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount = amount, currencyCode = it)
        } ?: currencyFormatter.formatCurrency(amount = amount)
    }
}
