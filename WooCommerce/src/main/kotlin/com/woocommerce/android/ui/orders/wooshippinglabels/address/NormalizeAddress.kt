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
        val response = repository.normalizeAddress(site.get(), address)
        val result = response.model
        return when {
            response.isError || result == null -> {
                val message =
                    response.error.message ?: if (result == null) "Empty response" else UNKNOWN_ERROR
                Result.failure(Exception(message))
            }

            result.errors != null -> Result.failure(NormalizeAddressException(errors = result.errors))
            else -> Result.success(result)
        }
    }
}

class NormalizeAddressException(val errors: Map<String, String>) : Exception() {
    val generalError: String?
        get() = errors[ERROR_GENERAL]
    val addressError: String?
        get() = errors[ERROR_ADDRESS]

    companion object {
        const val ERROR_GENERAL = "general"
        const val ERROR_ADDRESS = "address"
        const val UNKNOWN_ERROR = "Unknown error"
    }
}
