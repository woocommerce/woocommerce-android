package com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain

import com.woocommerce.android.R
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingSelectedRateModel
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
        return rates.groupBy { it.defaultRate.carrier }
            .map { (carrier, models) ->
                getCarrier(carrier) to models
                    .map { getShippingRate(it, resourceProvider, currencyCode) }
                    .filter { it.options.containsKey(Option.DEFAULT) }
            }
            .filter { it.second.isNotEmpty() }
            .toMap()
    }

    operator fun invoke(
        selectedRate: ShippingRateUI
    ): WooShippingSelectedRateModel {
        return WooShippingSelectedRateModel(
            rate = selectedRate.selectedRateOption.rate,
            parentRate = if (selectedRate.selectedOption != Option.DEFAULT) {
                selectedRate.defaultRate.rate
            } else {
                null
            },
            additionalRates = selectedRate.additionalSelectedOptions.map {
                selectedRate.options.getValue(it).rate
            }
        )
    }

    private fun getCarrier(carrier: WooShippingCarrier): CarrierUI {
        return when (carrier) {
            WooShippingCarrier.FEDEX -> CarrierUI(
                carrier = carrier,
                name = "FedEx",
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
        val options = rate.rateOptions.mapValues { rateOption ->
            when (rateOption.value.option) {
                Option.DEFAULT -> {
                    ShippingRateOptionUI(
                        optionName = "",
                        fee = BigDecimal.ZERO,
                        formattedFee = "",
                        feeDescription = "",
                        option = rateOption.value.option,
                        rate = rateOption.value
                    )
                }

                else -> {
                    val fee = rateOption.value.price.minus(rate.defaultRate.price)
                    val formattedFee = formatFee(fee, currencyCode)

                    ShippingRateOptionUI(
                        optionName = rateOption.value.option.getTitle(),
                        fee = fee,
                        formattedFee = formattedFee,
                        feeDescription = resourceProvider.getString(
                            R.string.woo_shipping_rate_surcharge_description_template,
                            rateOption.value.option.getTitle(),
                            formattedFee
                        ),
                        option = rateOption.value.option,
                        rate = rateOption.value,
                    )
                }
            }
        }

        return ShippingRateUI(
            title = rate.defaultRate.serviceName,
            formattedBasePrice = formatCurrency(rate.defaultRate.price, currencyCode),
            shippingRateIncludedOptions = getShippingRateOptionsList(rate.defaultRate, resourceProvider, currencyCode),
            formattedEstimatedDays = getEstimatedDays(rate.defaultRate, resourceProvider),
            options = options,
            selectedOption = Option.DEFAULT,
            additionalSelectedOptions = emptyList()
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
            else -> formatCurrency(price, currencyCode).let {
                if (price > BigDecimal.ZERO) {
                    resourceProvider.getString(R.string.woo_shipping_rate_extra_cost_format, it)
                } else {
                    it
                }
            }
        }
    }

    private fun formatCurrency(amount: BigDecimal, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount = amount, currencyCode = it)
        } ?: currencyFormatter.formatCurrency(amount = amount)
    }

    private fun Option.getTitle() = when (this) {
        Option.DEFAULT -> ""
        Option.SIGNATURE -> resourceProvider.getString(R.string.woo_shipping_rate_option_signature_required)
        Option.ADULT_SIGNATURE -> resourceProvider.getString(R.string.woo_shipping_rate_option_adult_signature_required)
        Option.CARBON_NEUTRAL -> resourceProvider.getString(R.string.woo_shipping_rate_option_carbon_neutral)
        Option.ADDITIONAL_HANDLING -> resourceProvider.getString(R.string.woo_shipping_rate_option_additional_handling)
        Option.SATURDAY_DELIVERY -> resourceProvider.getString(R.string.woo_shipping_rate_option_saturday_delivery)
    }
}
