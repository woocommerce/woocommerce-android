package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import javax.inject.Inject

class FetchServiceStatusNotices @Inject constructor(
    private val shippingRepository: WooShippingLabelRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Result<List<ServiceStatusNotice>> {
        val site = selectedSite.getOrNull() ?: return Result.failure(Exception("No site selected"))
        val response = shippingRepository.fetchServiceStatus(site)
        val error = response.error
        return when {
            error != null -> Result.failure(WooException(error))
            else -> Result.success(response.model.orEmpty())
        }
    }
}
