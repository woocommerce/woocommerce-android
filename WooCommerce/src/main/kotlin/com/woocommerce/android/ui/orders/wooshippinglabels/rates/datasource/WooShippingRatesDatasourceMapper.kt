package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingRatePurchaseDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingRatePurchaseResponseDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.ShippingRateDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesDTO
import java.math.BigDecimal
import javax.inject.Inject

class WooShippingRatesDatasourceMapper @Inject constructor() {
    companion object {
        private const val DEFAULT_RATE_OPTION = "default"
        private const val SIGNATURE_RATE_OPTION = "signature_required"
        private const val ADULT_SIGNATURE_RATE_OPTION = "adult_signature_required"

        private const val CARRIER_USPS_KEY = "usps"
        private const val CARRIER_UPS_KEY = "ups"
        private const val CARRIER_FEDEX_KEY = "fedex"
        private const val CARRIER_DHL_EXPRESS_KEY = "dhlexpress"
        private const val CARRIER_DHL_ECOMMERCE_KEY = "dhlecommerce"
        private const val CARRIER_DHL_ECOMMERCE_ASIA_KEY = "dhlecommerceasia"
    }

    operator fun invoke(
        packageId: String,
        shippingRateDTO: ShippingRateDTO,
        rateOptionId: String
    ): WooShippingRateModel {
        return WooShippingRateModel(
            packageId = packageId,
            shipmentId = shippingRateDTO.shipmentId.orEmpty(),
            rateId = shippingRateDTO.rateId,
            serviceId = shippingRateDTO.serviceId,
            carrierId = shippingRateDTO.carrierId.orEmpty(),
            serviceName = shippingRateDTO.title,
            deliveryDays = shippingRateDTO.deliveryDays,
            price = shippingRateDTO.rate,
            discount = shippingRateDTO.retailRate?.minus(shippingRateDTO.rate) ?: BigDecimal.ZERO,
            option = getOption(rateOptionId),
            carrier = getCarrier(shippingRateDTO.carrierId.orEmpty()),
            isTrackingEnabled = shippingRateDTO.tracking,
            hasFreePickup = shippingRateDTO.freePickup,
            insurance = shippingRateDTO.insurance?.toBigDecimalOrNull(),
            deliveryDate = shippingRateDTO.deliveryDate,
            isDeliveryDateGuaranteed = shippingRateDTO.deliveryDateGuaranteed,
            isSelected = shippingRateDTO.isSelected,
            listRate = shippingRateDTO.listRate ?: BigDecimal.ZERO,
            retailRate = shippingRateDTO.retailRate ?: BigDecimal.ZERO
        )
    }

    operator fun invoke(
        packageId: String,
        shippingRateDTO: ShippingRatePurchaseResponseDTO,
        rateOptionId: String
    ): WooShippingRateModel {
        return WooShippingRateModel(
            packageId = packageId,
            shipmentId = shippingRateDTO.shipmentId.orEmpty(),
            rateId = shippingRateDTO.rateId,
            serviceId = shippingRateDTO.serviceId,
            carrierId = shippingRateDTO.carrierId.orEmpty(),
            serviceName = shippingRateDTO.title,
            deliveryDays = shippingRateDTO.deliveryDays,
            price = shippingRateDTO.rate,
            discount = shippingRateDTO.retailRate?.minus(shippingRateDTO.rate) ?: BigDecimal.ZERO,
            option = getOption(rateOptionId),
            carrier = getCarrier(shippingRateDTO.carrierId.orEmpty()),
            isTrackingEnabled = shippingRateDTO.tracking,
            hasFreePickup = shippingRateDTO.freePickup,
            insurance = shippingRateDTO.insurance?.toBigDecimalOrNull(),
            deliveryDate = shippingRateDTO.deliveryDate,
            isDeliveryDateGuaranteed = shippingRateDTO.deliveryDateGuaranteed,
            isSelected = shippingRateDTO.isSelected,
            listRate = shippingRateDTO.listRate ?: BigDecimal.ZERO,
            retailRate = shippingRateDTO.retailRate ?: BigDecimal.ZERO
        )
    }

    operator fun invoke(
        packageId: String,
        shippingRateDTO: ShippingRatePurchaseDTO
    ): WooShippingRateModel {
        return invoke(packageId, shippingRateDTO.rate, DEFAULT_RATE_OPTION)
    }

    operator fun invoke(response: Map<String, Map<String, WooShippingRatesDTO>>?): List<WooShippingRateOptionsModel> {
        val optionsMap = mutableMapOf<String, MutableList<WooShippingRateModel>>()
        response?.forEach { (packageId, ratesMap) ->
            ratesMap.forEach { (rateOptionId, wooShippingRates) ->
                wooShippingRates.rates.forEach { rate ->
                    val option = WooShippingRateModel(
                        packageId = packageId,
                        shipmentId = rate.shipmentId.orEmpty(),
                        rateId = rate.rateId,
                        serviceId = rate.serviceId,
                        carrierId = rate.carrierId.orEmpty(),
                        serviceName = rate.title,
                        deliveryDays = rate.deliveryDays,
                        price = rate.rate,
                        discount = rate.retailRate?.minus(rate.rate) ?: BigDecimal.ZERO,
                        option = getOption(rateOptionId),
                        carrier = getCarrier(rate.carrierId.orEmpty()),
                        isTrackingEnabled = rate.tracking,
                        hasFreePickup = rate.freePickup,
                        insurance = rate.insurance?.toBigDecimalOrNull(),
                        deliveryDate = rate.deliveryDate,
                        isDeliveryDateGuaranteed = rate.deliveryDateGuaranteed,
                        isSelected = rate.isSelected,
                        listRate = rate.listRate ?: BigDecimal.ZERO,
                        retailRate = rate.retailRate ?: BigDecimal.ZERO
                    )
                    optionsMap.getOrPut(key = option.serviceId, defaultValue = { mutableListOf() }).add(option)
                }
            }
        }
        return optionsMap.mapNotNull {
            if (it.value.isEmpty()) {
                return@mapNotNull null
            }
            WooShippingRateOptionsModel(
                rateOptions = it.value.associateBy { rate -> rate.option }
            )
        }
    }

    private fun getOption(rateOptionId: String): Option {
        return when (rateOptionId) {
            DEFAULT_RATE_OPTION -> Option.DEFAULT
            SIGNATURE_RATE_OPTION -> Option.SIGNATURE
            ADULT_SIGNATURE_RATE_OPTION -> Option.ADULT_SIGNATURE
            else -> Option.DEFAULT
        }
    }

    private fun getCarrier(carrierId: String) =
        when (carrierId) {
            CARRIER_USPS_KEY -> WooShippingCarrier.USPS
            CARRIER_FEDEX_KEY -> WooShippingCarrier.FEDEX
            CARRIER_UPS_KEY -> WooShippingCarrier.UPS
            CARRIER_DHL_EXPRESS_KEY, CARRIER_DHL_ECOMMERCE_KEY, CARRIER_DHL_ECOMMERCE_ASIA_KEY -> WooShippingCarrier.DHL
            else -> WooShippingCarrier.UNKNOWN
        }
}
