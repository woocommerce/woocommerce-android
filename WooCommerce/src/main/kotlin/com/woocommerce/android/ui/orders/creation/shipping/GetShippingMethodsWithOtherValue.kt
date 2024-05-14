package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import javax.inject.Inject

class GetShippingMethodsWithOtherValue @Inject constructor(
    private val shippingMethodsRepository: ShippingMethodsRepository
) {
    suspend operator fun invoke(): Result<List<ShippingMethod>> {
        val result = shippingMethodsRepository.fetchShippingMethods()
        return when {
            result.model != null -> {
                val shippingMethodsWithOtherValue = result.model!!.toMutableList().also {
                    it.add(shippingMethodsRepository.getOtherShippingMethod())
                }

                Result.success(shippingMethodsWithOtherValue)
            }

            else -> {
                val message = result.error.message ?: "error fetching shipping methods"
                Result.failure(Exception(message))
            }
        }
    }
}
