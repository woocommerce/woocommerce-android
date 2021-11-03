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
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
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
                country = addressViewModel.countryLocation.code,
                state = if (shouldShowStateSpinner()) {
                    addressViewModel.stateLocation.code
                } else {
                    stateEditText.text
                }
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBaseEditAddressBinding.bind(view)
        storedAddress.bindToView()
        bindTextWatchers()

        addressViewModel.start(
            countryCode = storedAddress.country,
            stateCode = storedAddress.state
        )

        binding.countrySpinner.setClickListener {
            showCountrySelectorDialog()
        }

        binding.stateSpinner.setClickListener {
            showStateSelectorDialog()
        }

        setupObservers()
        setupResultHandlers()
        onViewBound(binding)
        updateStateViews()
    }

    override fun hasChanges() =
        (addressDraft != storedAddress) || binding.replicateAddressSwitch.isChecked

    override fun onStop() {
        sharedViewModel.onReplicateAddressSwitchChanged(false)
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
        super.onStop()
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
        binding.countrySpinner.setText(getCountryLabelByCountryCode())
        binding.stateSpinner.setText(addressViewModel.stateLocation.name)
        binding.stateEditText.text = state
        binding.replicateAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.onReplicateAddressSwitchChanged(isChecked)
            updateDoneMenuItem()
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
        binding.stateEditText.textWatcher = textWatcher
    }

    private fun shouldShowStateSpinner() = addressViewModel.hasStates()

    /**
     * When the country is empty, or we don't have country or state data, we show an editText
     * for the state rather than a spinner
     */
    private fun updateStateViews() {
        binding.stateSpinner.isVisible = shouldShowStateSpinner()
        binding.stateEditText.isVisible = !shouldShowStateSpinner()
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
            new.countryLocation.takeIfNotEqualTo(old?.countryLocation) {
                binding.countrySpinner.setText(it.name)
                updateDoneMenuItem()
                updateStateViews()
            }
            new.stateLocation.takeIfNotEqualTo(old?.stateLocation) {
                binding.stateSpinner.setText(it.name)
                binding.stateEditText.text = it.code
                updateDoneMenuItem()
            }
            new.isLoading.takeIfNotEqualTo(old?.isLoading) {
                binding.progressBar.isVisible = it
                if (old?.isLoading == true) {
                    updateStateViews()
                }
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
