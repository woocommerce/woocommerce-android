package com.woocommerce.android.ui.orders.wooshippinglabels.address.destination

import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateDestinationAddress @Inject constructor(
    private val shippingRepository: WooShippingLabelRepository,
    private val selectedSite: SelectedSite,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(
        address: Address,
        orderId: Long
    ): Result<DestinationShippingAddress> {
        return withContext(coroutineDispatchers.io) {
            selectedSite.getOrNull()?.let {
                val response = shippingRepository.updateDestinationAddress(it, orderId, address)
                val result = response.model
                when {
                    response.isError || result == null -> {
                        val message =
                            response.error.message
                                ?: if (result == null) "Empty response" else "Unknown error"
                        Result.failure(Exception(message))
                    }

                    else -> Result.success(result)
                }
            } ?: Result.failure(Exception("No site selected"))
        }
    }
}
