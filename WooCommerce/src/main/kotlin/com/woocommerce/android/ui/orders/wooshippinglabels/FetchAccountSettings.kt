package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import javax.inject.Inject

class FetchAccountSettings @Inject constructor(
    private val shippingRepository: WooShippingLabelRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Result<StoreOptionsModel> {
        return selectedSite.getOrNull()?.let {
            val response = shippingRepository.fetchAccountSettings(it)
            val result = response.model
            when {
                response.isError.not() && result != null && result != StoreOptionsModel.EMPTY -> {
                    Result.success(result)
                }

                else -> {
                    val message = response.error?.message ?: "Unknown error"
                    Result.failure(Exception(message))
                }
            }
        } ?: Result.failure(Exception("No site selected"))
    }
}
