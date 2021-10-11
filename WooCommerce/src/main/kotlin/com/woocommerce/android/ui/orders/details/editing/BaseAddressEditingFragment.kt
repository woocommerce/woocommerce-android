package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address

abstract class BaseAddressEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_base_edit_address) {
    companion object {
        const val TAG = "BaseEditAddressFragment"
    }

    abstract val address: Address

    private var _binding: FragmentBaseEditAddressBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        address.bindToView()
    }

    private fun Address.bindToView() {
        binding.firstName.setText(firstName)
        binding.lastName.setText(lastName)
        binding.email.setText(email)
        binding.phone.setText(phone)
        binding.company.setText(company)
        binding.address1.setText(address1)
        binding.address2.setText(address2)
        binding.city.setText(city)
        binding.postcode.setText(postcode)
    }
}
