package com.woocommerce.android.ui.orders.details.editing.address

import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address

class ShippingAddressEditingFragment : BaseAddressEditingFragment() {
    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_SHIPPING_ADDRESS

    override val storedAddress: Address
        get() = sharedViewModel.order.shippingAddress

    override fun saveChanges() = sharedViewModel.updateShippingAddress(addressDraft)

    override fun getFragmentTitle() = getString(R.string.order_detail_shipping_address_section)

    override fun onViewBound(binding: FragmentBaseEditAddressBinding) {
        binding.email.visibility = View.GONE
        binding.addressSectionHeader.text = getString(R.string.order_detail_shipping_address_section)
        binding.replicateAddressSwitch.text = getString(R.string.order_detail_use_as_billing_address)
        binding.replicateAddressSwitch.visibility = View.VISIBLE
        sharedViewModel.order.billingAddress.copy(email = "")
            .bindAsAddressReplicationToggleState()
    }
}
