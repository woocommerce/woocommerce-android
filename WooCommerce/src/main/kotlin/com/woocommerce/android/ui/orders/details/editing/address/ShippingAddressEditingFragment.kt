package com.woocommerce.android.ui.orders.details.editing.address

import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.main.AppBarStatus

class ShippingAddressEditingFragment : BaseAddressEditingFragment() {
    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_SHIPPING_ADDRESS

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

    override val storedAddress: Address
        get() = sharedViewModel.order.shippingAddress

    override val addressType: AddressViewModel.AddressType = AddressViewModel.AddressType.SHIPPING

    override fun saveChanges() = sharedViewModel.updateShippingAddress(addressDraft)

    override fun getFragmentTitle() = getString(R.string.order_detail_shipping_address_section)

    override fun onViewBound(binding: FragmentBaseEditAddressBinding) {
        binding.form.email.visibility = View.GONE
        binding.form.addressSectionHeader.text = getString(R.string.order_detail_shipping_address_section)
        replicateAddressSwitch.text = getString(R.string.order_detail_use_as_billing_address)
    }
}
