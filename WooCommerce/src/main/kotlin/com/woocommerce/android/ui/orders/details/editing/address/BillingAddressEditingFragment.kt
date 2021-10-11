package com.woocommerce.android.ui.orders.details.editing.address

import com.woocommerce.android.R
import com.woocommerce.android.model.Address

class BillingAddressEditingFragment : BaseAddressEditingFragment() {
    override val storedAddress: Address
        get() = sharedViewModel.order.billingAddress

    override fun saveChanges() =
        sharedViewModel.updateBillingAddress(addressDraft)

    override fun getFragmentTitle() = getString(R.string.order_detail_billing_address_section)
}
