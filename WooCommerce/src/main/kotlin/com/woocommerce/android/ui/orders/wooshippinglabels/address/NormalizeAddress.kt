package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.address.NormalizeAddressException.Companion.UNKNOWN_ERROR
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
                    Result.failure(NormalizeAddressException(response.error.message ?: GENERAL_ERROR))
                }

                else -> Result.success(result)
            }
        } ?: Result.failure(NormalizeAddressException(GENERAL_ERROR))
    }
}

class NormalizeAddressException(val error: String) : Exception(error) {
    companion object {
        const val ERROR_GENERAL = "general"
        const val ERROR_ADDRESS = "address"
        const val UNKNOWN_ERROR = "Unknown error"
    }
}
