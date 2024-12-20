package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import javax.inject.Inject

class PurchaseShippingLabel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooShippingLabelRepository: WooShippingLabelRepository
) {
    @Suppress("LongParameterList")
    suspend operator fun invoke(
        orderId: Long,
        shippableItems: List<Long>,
        selectedPackage: PackageData,
        shipTo: Address,
        shipFrom: OriginShippingAddress,
        shippingRate: WooShippingRateModel,
        weight: Float,
        lastOrderComplete: Boolean,
    ): Result<PurchasedLabelData> {
        return selectedSite.getOrNull()?.let {
            val response = wooShippingLabelRepository.purchaseShippingLabel(
                orderId = orderId,
                shippableItems = shippableItems,
                selectedPackage = selectedPackage,
                shipTo = shipTo,
                shipFrom = shipFrom,
                selectedRate = shippingRate,
                weight = weight,
                lastOrderComplete = lastOrderComplete,
                site = it
            )
            val result = response.model
            if (response.isError || result == null) {
                Result.failure(Exception("Purchase failed"))
            } else {
                Result.success(result)
            }
        } ?: Result.failure(Exception("No site selected"))
    }
}
