package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import javax.inject.Inject

class GetShippingMethodById @Inject constructor(
    private val shippingMethodsRepository: ShippingMethodsRepository
) {
    suspend operator fun invoke(shippingMethodId: String?): ShippingMethod {
        return when {
            shippingMethodId.isNullOrEmpty() -> shippingMethodsRepository.getNAShippingMethod()
            shippingMethodId == ShippingMethodsRepository.OTHER_ID -> shippingMethodsRepository.getOtherShippingMethod()
            else -> {
                val result = shippingMethodsRepository.getShippingMethodById(shippingMethodId)
                    ?: shippingMethodsRepository.fetchShippingMethodByIdAndSaveResult(shippingMethodId).model
                result ?: shippingMethodsRepository.getOtherShippingMethod()
            }
        }
    }
}
