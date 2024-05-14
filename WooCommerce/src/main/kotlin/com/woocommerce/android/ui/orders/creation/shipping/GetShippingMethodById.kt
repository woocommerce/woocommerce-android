package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import javax.inject.Inject

class GetShippingMethodById @Inject constructor(
    private val shippingMethodsRepository: ShippingMethodsRepository
) {
    suspend operator fun invoke(shippingMethodId: String?): ShippingMethod {
        val other = shippingMethodsRepository.getOtherShippingMethod()
        if (shippingMethodId == ShippingMethodsRepository.OTHER_ID || shippingMethodId == null) {
            return other
        }
        val result = shippingMethodsRepository.fetchShippingMethodById(shippingMethodId)
        return result.model ?: other
    }
}
