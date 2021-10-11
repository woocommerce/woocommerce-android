package com.woocommerce.android.ui.orders.details.editing

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.details.editing.address.BaseAddressEditingFragment

class BillingAddressEditingFragment : BaseAddressEditingFragment() {
    override val address: Address
        get() = sharedViewModel.order.billingAddress

    override fun hasChanges(): Boolean {
        TODO("Not yet implemented")
    }

    override fun saveChanges(): Boolean {
        TODO("Not yet implemented")
    }
}
