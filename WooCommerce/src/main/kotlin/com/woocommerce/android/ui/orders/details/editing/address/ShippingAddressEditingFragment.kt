package com.woocommerce.android.ui.orders.details.editing.address

import com.woocommerce.android.model.Address

class ShippingAddressEditingFragment : BaseAddressEditingFragment() {
    override val storedAddress: Address
        get() = sharedViewModel.order.shippingAddress

    override fun saveChanges() =
        sharedViewModel.updateShippingAddress(addressDraft)
}
