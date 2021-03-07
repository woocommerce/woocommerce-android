package com.woocommerce.android.ui.orders.creation.addcustomer

import com.woocommerce.android.util.CoroutineDispatchers
import javax.inject.Inject

class AddCustomerRepository @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val customerStore: WCCustomerStore
) {
}
