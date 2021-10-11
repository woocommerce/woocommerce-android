package com.woocommerce.android.ui.orders.details.editing.address

import com.woocommerce.android.model.Address

class ShippingAddressEditingFragment : BaseAddressEditingFragment() {
    override val address: Address
        get() = sharedViewModel.order.shippingAddress

    override fun hasChanges(): Boolean {
        TODO("Not yet implemented")
    }

    override fun saveChanges(): Boolean {
        TODO("Not yet implemented")
    }
}
