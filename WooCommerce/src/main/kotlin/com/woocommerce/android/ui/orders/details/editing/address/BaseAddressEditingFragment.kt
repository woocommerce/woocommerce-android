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
                firstName = firstName.text,
                lastName = lastName.text,
                email = email.text,
                phone = phone.text,
                company = company.text,
                address1 = address1.text,
                address2 = address2.text,
                city = city.text,
                postcode = postcode.text,
                // temporary field assignments, must be replaced with actual input
                country = storedAddress.country,
                state = storedAddress.state
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBaseEditAddressBinding.bind(view)
        storedAddress.bindToView()
        bindTextWatchers()
    }

    override fun hasChanges() = addressDraft != storedAddress

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onDestroyView() {
        removeTextWatchers()
        _binding = null
        super.onDestroyView()
    }

    private fun Address.bindToView() {
        binding.firstName.text = firstName
        binding.lastName.text = lastName
        binding.email.text = email
        binding.phone.text = phone
        binding.company.text = company
        binding.address1.text = address1
        binding.address2.text = address2
        binding.city.text = city
        binding.postcode.text = postcode
    }

    private fun bindTextWatchers() {
        binding.firstName.textWatcher = textWatcher
        binding.lastName.textWatcher = textWatcher
        binding.email.textWatcher = textWatcher
        binding.phone.textWatcher = textWatcher
        binding.company.textWatcher = textWatcher
        binding.address1.textWatcher = textWatcher
        binding.address2.textWatcher = textWatcher
        binding.city.textWatcher = textWatcher
        binding.postcode.textWatcher = textWatcher
    }

    private fun removeTextWatchers() {
        binding.firstName.removeCurrentTextWatcher()
        binding.lastName.removeCurrentTextWatcher()
        binding.email.removeCurrentTextWatcher()
        binding.phone.removeCurrentTextWatcher()
        binding.company.removeCurrentTextWatcher()
        binding.address1.removeCurrentTextWatcher()
        binding.address2.removeCurrentTextWatcher()
        binding.city.removeCurrentTextWatcher()
        binding.postcode.removeCurrentTextWatcher()
    }
}
