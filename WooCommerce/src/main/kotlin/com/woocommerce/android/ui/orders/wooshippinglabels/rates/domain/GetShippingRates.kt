package com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRatesRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import javax.inject.Inject

class GetShippingRates @Inject constructor(
    private val repository: WooShippingRatesRepository,
    private val shippingMapper: WooShippingRatesDomainMapper
) {

    @Suppress("LongParameterList", "TooGenericExceptionCaught")
    suspend operator fun invoke(
        orderId: Long,
        selectedPackage: PackageData,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        weight: Float,
        currencyCode: String?,
        customsData: CustomsData?,
        hazmatSelection: ShippingLabelHazmatCategory? = null
    ): Result<Map<CarrierUI, List<ShippingRateUI>>> = repository.getShippingRates(
        orderId = orderId,
        selectedPackage = selectedPackage,
        shipTo = shipTo,
        shipFrom = shipFrom,
        weight = weight,
        customsData = customsData,
        hazmatSelection = hazmatSelection
    ).fold(
        onSuccess = { ratesResponse ->
            if (ratesResponse.isEmpty()) {
                val message = if (hazmatSelection == null) {
                    R.string.woo_shipping_labels_package_creation_shipping_rates_empty
                } else {
                    R.string.woo_shipping_labels_package_creation_shipping_rates_empty_with_hazmat
                }

                Result.failure(NoAvailableRatesException(message))
            } else {
                try {
                    val mappedRates = shippingMapper(ratesResponse, currencyCode)
                    Result.success(mappedRates)
                } catch (e: Exception) {
                    Result.failure(Exception("Failed to map shipping rates", e))
                }
            }
        },
        onFailure = { exception ->
            Result.failure(exception)
        }
    )
}

class NoAvailableRatesException(@StringRes val messageResId: Int) : Exception("No available rates")

class InvalidDestinationNameRateException : Exception("Invalid destination name")
