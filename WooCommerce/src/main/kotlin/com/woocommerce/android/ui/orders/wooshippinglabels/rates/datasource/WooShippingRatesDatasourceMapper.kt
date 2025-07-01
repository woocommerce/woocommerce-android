package com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource

import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingCarrier
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingRatePurchaseDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingRatePurchaseResponseDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel.Option
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.networking.WooShippingRatesDTO
import java.math.BigDecimal
import javax.inject.Inject

class WooShippingRatesDatasourceMapper @Inject constructor() {
    companion object {
        const val CARRIER_DHL_EXPRESS_KEY = "dhlexpress"
    }

    operator fun invoke(
        packageId: String,
        shippingRateDTO: ShippingRatePurchaseResponseDTO,
        rateOptionId: String
    ): WooShippingRateModel? {
        val rateOption = Option.fromId(rateOptionId) ?: return null
        return WooShippingRateModel(
            packageId = packageId,
            shipmentId = shippingRateDTO.shipmentId.orEmpty(),
            rateId = shippingRateDTO.rateId,
            serviceId = shippingRateDTO.serviceId,
            carrierId = shippingRateDTO.carrierId.orEmpty(),
            serviceName = shippingRateDTO.title,
            deliveryDays = shippingRateDTO.deliveryDays,
            price = shippingRateDTO.rate,
            option = rateOption,
            carrier = WooShippingCarrier.fromCarrierId(shippingRateDTO.carrierId.orEmpty()),
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
    ): WooShippingRateModel? {
        return invoke(packageId, shippingRateDTO.rate, Option.DEFAULT.id)
    }

    operator fun invoke(response: Map<String, Map<String, WooShippingRatesDTO>>?): List<WooShippingRateOptionsModel> {
        val optionsMap = mutableMapOf<String, MutableList<WooShippingRateModel>>()
        response?.forEach { (packageId, ratesMap) ->
            ratesMap.forEach nestedForEach@{ (rateOptionId, wooShippingRates) ->
                val rateOption = Option.fromId(rateOptionId) ?: return@nestedForEach
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
                        option = rateOption,
                        carrier = WooShippingCarrier.fromCarrierId(rate.carrierId.orEmpty()),
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
}
