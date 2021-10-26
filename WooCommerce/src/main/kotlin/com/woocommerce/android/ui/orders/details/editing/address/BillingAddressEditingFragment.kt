package com.woocommerce.android.ui.orders.details.editing.address

import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address

class BillingAddressEditingFragment : BaseAddressEditingFragment() {
    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_BILLING_ADDRESS

    override val storedAddress: Address
        get() = sharedViewModel.order.billingAddress

    override fun saveChanges() = sharedViewModel.updateBillingAddress(addressDraft)

    override fun getFragmentTitle() = getString(R.string.order_detail_billing_address_section)

    override fun onViewBound(binding: FragmentBaseEditAddressBinding) {
        binding.addressSectionHeader.text = getString(R.string.order_detail_billing_address_section)
        binding.replicateAddressSwitch.text = getString(R.string.order_detail_use_as_shipping_address)
    }
}
