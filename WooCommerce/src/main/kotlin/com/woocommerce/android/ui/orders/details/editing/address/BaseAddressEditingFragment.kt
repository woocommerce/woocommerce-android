package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.BaseOrderEditingFragment
import org.wordpress.android.util.ActivityUtils

abstract class BaseAddressEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_base_edit_address) {
    companion object {
        const val TAG = "BaseEditAddressFragment"
        const val SELECT_COUNTRY_REQUEST = "select_country_request"
        const val SELECT_STATE_REQUEST = "select_state_request"
    }

    private val addressViewModel by hiltNavGraphViewModels<AddressViewModel>(R.id.nav_graph_orders)

    abstract val storedAddress: Address
    abstract fun onViewBound(binding: FragmentBaseEditAddressBinding)

    private var _binding: FragmentBaseEditAddressBinding? = null
    private val binding get() = _binding!!

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
                country = countrySpinner.getText(),
                state = storedAddress.state // TODO nbradbury add state spinner
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBaseEditAddressBinding.bind(view)
        storedAddress.bindToView()
        bindTextWatchers()

        addressViewModel.start(
            country = storedAddress.country,
            state = storedAddress.state
        )

        binding.countrySpinner.setClickListener {
            showCountrySelectorDialog()
        }

        setupObservers()
        setupResultHandlers()
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
        binding.firstName.text = firstName
        binding.lastName.text = lastName
        binding.email.text = email
        binding.phone.text = phone
        binding.company.text = company
        binding.address1.text = address1
        binding.address2.text = address2
        binding.city.text = city
        binding.postcode.text = postcode
        // TODO nbradbury show country name instead of country code
        binding.countrySpinner.setText(country)
        binding.replicateAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.onReplicateAddressSwitchChanged(isChecked)
        }
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

    internal fun Address.bindAsAddressReplicationToggleState() {
        (this == storedAddress)
            .apply { binding.replicateAddressSwitch.isChecked = this }
            .also { sharedViewModel.onReplicateAddressSwitchChanged(it) }
    }

    private fun showCountrySelectorDialog() {
        val countries = addressViewModel.countries
        val action = OrderDetailFragmentDirections.actionGlobalItemSelectorDialog(
            addressDraft.country,
            countries.map { it.name }.toTypedArray(),
            countries.map { it.code }.toTypedArray(),
            SELECT_COUNTRY_REQUEST,
            getString(R.string.shipping_label_edit_address_country)
        )
        findNavController().navigateSafely(action)
    }

    @Suppress("UnusedPrivateMember")
    private fun showStateSelectorDialog() {
        // TODO nbradbury check we have a country first
        val states = addressViewModel.states
        val action = OrderDetailFragmentDirections.actionGlobalItemSelectorDialog(
            addressDraft.state,
            states.map { it.name }.toTypedArray(),
            states.map { it.code }.toTypedArray(),
            SELECT_STATE_REQUEST,
            getString(R.string.shipping_label_edit_address_state)
        )
        findNavController().navigateSafely(action)
    }

    private fun setupObservers() {
        addressViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.country.takeIfNotEqualTo(old?.country) {
                binding.countrySpinner.setText(new.country)
                updateDoneMenuItem()
            }
            new.state.takeIfNotEqualTo(old?.state) {
                // TODO nbradbury update displayed state
            }
            new.isLoading.takeIfNotEqualTo(old?.isLoading) { isLoading ->
                binding.progressBar.isVisible = new.isLoading
            }
        }
    }

    private fun setupResultHandlers() {
        handleResult<String>(SELECT_COUNTRY_REQUEST) {
            addressViewModel.onCountrySelected(it)
        }
        handleResult<String>(SELECT_STATE_REQUEST) {
            addressViewModel.onStateSelected(it)
        }
    }
}
