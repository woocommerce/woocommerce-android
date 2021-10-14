package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.details.editing.BaseOrderEditingFragment
import org.wordpress.android.util.ActivityUtils

abstract class BaseAddressEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_base_edit_address) {
    companion object {
        const val TAG = "BaseEditAddressFragment"
    }

    private var _binding: FragmentBaseEditAddressBinding? = null
    private val binding get() = _binding!!

    abstract val storedAddress: Address

    val addressDraft
        get() = binding.run {
            Address(
                firstName = firstName.getText(),
                lastName = lastName.getText(),
                email = email.getText(),
                phone = phone.getText(),
                company = company.getText(),
                address1 = address1.getText(),
                address2 = address2.getText(),
                city = city.getText(),
                postcode = postcode.getText(),
                // temporary field assignments, must be replaced with actual input
                country = storedAddress.country,
                state = storedAddress.state
            )
        }

    abstract fun onViewBound(binding: FragmentBaseEditAddressBinding)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBaseEditAddressBinding.bind(view)
        storedAddress.bindToView()
        onViewBound(binding)
    }

    override fun hasChanges() = addressDraft != storedAddress

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
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
        binding.useAsAnotherAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.onUseAsOtherAddressSwitchChanged(isChecked)
        }
    }
}
