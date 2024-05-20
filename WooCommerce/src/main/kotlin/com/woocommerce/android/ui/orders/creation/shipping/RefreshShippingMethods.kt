package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import javax.inject.Inject

class RefreshShippingMethods @Inject constructor(
    private val shippingMethodsRepository: ShippingMethodsRepository
) {
    suspend operator fun invoke(): Result<List<ShippingMethod>> {
        val result = shippingMethodsRepository.fetchShippingMethodsAndSaveResults()
        return if (result.model != null && result.isError.not()) {
            Result.success(result.model!!)
        } else {
            Result.failure(Exception("Failure fetching shipping methods"))
        }
    }
}
