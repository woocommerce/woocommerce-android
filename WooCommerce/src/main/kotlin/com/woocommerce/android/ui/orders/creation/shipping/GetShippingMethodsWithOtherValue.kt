package com.woocommerce.android.ui.orders.creation.shipping

import com.woocommerce.android.model.ShippingMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetShippingMethodsWithOtherValue @Inject constructor(
    private val shippingMethodsRepository: ShippingMethodsRepository
) {
    operator fun invoke(): Flow<List<ShippingMethod>> {
        return shippingMethodsRepository.observeShippingMethods()
            .map { list ->
                val withOtherValue = list.toMutableList()
                    .also { it.add(shippingMethodsRepository.getOtherShippingMethod()) }
                withOtherValue
            }
    }
}
