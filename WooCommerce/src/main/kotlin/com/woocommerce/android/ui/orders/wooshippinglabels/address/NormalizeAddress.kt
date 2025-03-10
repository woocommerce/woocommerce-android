package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import javax.inject.Inject

class NormalizeAddress @Inject constructor(
    private val repository: WooShippingLabelRepository,
    private val site: SelectedSite,
) {
    suspend operator fun invoke(address: Address): Result<AddressNormalizationModel> {
        return site.getOrNull()?.let {
            val response = repository.normalizeAddress(it, address)
            val result = response.model
            when {
                response.isError || result == null -> {
                    val message =
                        response.error.message ?: if (result == null) "Empty response" else "Unknown error"
                    Result.failure(Exception(message))
                }
                else -> Result.success(result)
            }
        } ?: Result.failure(Exception("No site selected"))
    }
}
