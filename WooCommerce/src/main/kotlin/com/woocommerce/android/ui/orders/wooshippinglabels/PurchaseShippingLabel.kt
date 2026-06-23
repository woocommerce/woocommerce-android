package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.model.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.WooShippingRatesDomainMapper
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import javax.inject.Inject

class PurchaseShippingLabel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooShippingLabelRepository: WooShippingLabelRepository,
    private val ratesMapper: WooShippingRatesDomainMapper
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        orderId: Long,
        shippableItems: List<Long>,
        selectedPackage: PackageData,
        shipmentId: Int,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        shippingRate: ShippingRateUI,
        weight: Float,
        lastOrderCompleted: Boolean,
        customsData: CustomsData? = null,
        hazmatSelection: ShippingLabelHazmatCategory? = null
    ): Result<PurchasedLabelData> {
        val response = wooShippingLabelRepository.purchaseShippingLabel(
            orderId = orderId,
            shippableItems = shippableItems,
            selectedPackage = selectedPackage,
            shipmentId = shipmentId,
            shipTo = shipTo,
            shipFrom = shipFrom,
            selectedRate = ratesMapper(shippingRate),
            weight = weight,
            lastOrderCompleted = lastOrderCompleted,
            customsData = customsData,
            hazmatSelection = hazmatSelection,
            site = selectedSite.get()
        )

        val result = response.model

        return if (response.isError) {
            Result.failure(WooException(response.error))
        } else if (result == null) {
            Result.failure(Exception("Unknown error occurred while purchasing shipping label"))
        } else {
            Result.success(result)
        }
    }
}
