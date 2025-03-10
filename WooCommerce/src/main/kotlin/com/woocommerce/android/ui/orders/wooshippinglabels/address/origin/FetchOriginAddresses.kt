package com.woocommerce.android.ui.orders.wooshippinglabels.address.origin

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import javax.inject.Inject

class FetchOriginAddresses @Inject constructor(
    private val shippingRepository: WooShippingLabelRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Result<List<OriginShippingAddress>> {
        return selectedSite.getOrNull()?.let {
            val response = shippingRepository.fetchOriginAddresses(it)
            val result = response.model
            when {
                response.isError.not() && !result.isNullOrEmpty() -> {
                    Result.success(result)
                }

                else -> {
                    val message =
                        response.error?.message ?: if (result.isNullOrEmpty()) "Empty result" else "Unknown error"
                    Result.failure(Exception(message))
                }
            }
        } ?: Result.failure(Exception("No site selected"))
    }
}
