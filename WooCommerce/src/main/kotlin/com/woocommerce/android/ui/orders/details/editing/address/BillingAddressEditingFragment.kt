package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.main.AppBarStatus

class BillingAddressEditingFragment : BaseAddressEditingFragment() {
    private val args by navArgs<BillingAddressEditingFragmentArgs>()

    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_BILLING_ADDRESS

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override val storedAddress: Address by lazy {
        args.storedAddress
    }

    override val addressType: AddressViewModel.AddressType = AddressViewModel.AddressType.BILLING

    override fun saveChanges() = sharedViewModel.updateBillingAddress(addressDraft)

    override fun getFragmentTitle() = getString(R.string.order_detail_billing_address_section)

    override fun onViewBound(binding: FragmentBaseEditAddressBinding) {
        binding.form.addressSectionHeader.text = getString(R.string.order_detail_billing_address_section)
        replicateAddressSwitch.text = getString(R.string.order_detail_use_as_shipping_address)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.title = getString(R.string.order_detail_billing_address_section)
    }
}
