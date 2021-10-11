package com.woocommerce.android.ui.orders.details.editing.address

import com.woocommerce.android.model.Address

class BillingAddressEditingFragment : BaseAddressEditingFragment() {
    override val storedAddress: Address
        get() = sharedViewModel.order.billingAddress

    override fun saveChanges(): Boolean {
        TODO("Not yet implemented")
    }
}
