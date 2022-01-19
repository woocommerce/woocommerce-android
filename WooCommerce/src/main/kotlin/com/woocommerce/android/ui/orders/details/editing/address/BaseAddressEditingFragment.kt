package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.UiDimen
import com.woocommerce.android.ui.orders.creation.views.update
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.BaseOrderEditingFragment
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.util.UiHelpers.getPxOfUiDimen
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
    abstract val addressType: AddressViewModel.AddressType
    abstract fun onViewBound(binding: FragmentBaseEditAddressBinding)

    private var _binding: FragmentBaseEditAddressBinding? = null
    private val binding get() = _binding!!

    protected lateinit var replicateAddressSwitch: SwitchMaterial

    @Deprecated("Rely on state from ViewModel rather than binding")
    val addressDraft
        get() = binding.form.run {
            val addressState =
                addressViewModel.viewStateData.liveData.value!!.addressSelectionStates.getValue(addressType).address
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
                country = addressState.country,
                state = addressState.state
            )
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBaseEditAddressBinding.bind(view)
        replicateAddressSwitch = SwitchMaterial(requireContext())
        _binding?.form?.root?.addView(replicateAddressSwitch)
        replicateAddressSwitch.apply {
            val verticalMargin = getPxOfUiDimen(context, UiDimen.UiDimenRes(R.dimen.major_100))
            (layoutParams as ViewGroup.MarginLayoutParams).updateMargins(
                left = verticalMargin,
                right = verticalMargin
            )
        }

        replicateAddressSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedViewModel.onReplicateAddressSwitchChanged(isChecked)
            updateDoneMenuItem()
        }
        bindTextWatchers()

        addressViewModel.start(
            mapOf(addressType to storedAddress)
        )

        binding.form.countrySpinner.setClickListener {
            addressViewModel.onCountrySpinnerClicked(addressType)
        }

        binding.form.stateSpinner.setClickListener {
            addressViewModel.onStateSpinnerClicked(addressType)
        }

        addressViewModel.start(
            mapOf(addressType to storedAddress)
        )

        setupObservers()
        setupResultHandlers()
        onViewBound(binding)
    }

    override fun hasChanges() =
        (addressDraft != storedAddress) || replicateAddressSwitch.isChecked

    override fun onStop() {
        sharedViewModel.onReplicateAddressSwitchChanged(false)
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
        super.onStop()
    }

    override fun onDetach() {
//        addressViewModel.onScreenDetached()
        super.onDetach()
    }

    private fun bindTextWatchers() {
        binding.form.firstName.textWatcher = textWatcher
        binding.form.lastName.textWatcher = textWatcher
        binding.form.email.textWatcher = textWatcher
        binding.form.phone.textWatcher = textWatcher
        binding.form.company.textWatcher = textWatcher
        binding.form.address1.textWatcher = textWatcher
        binding.form.address2.textWatcher = textWatcher
        binding.form.city.textWatcher = textWatcher
        binding.form.postcode.textWatcher = textWatcher
        binding.form.stateEditText.textWatcher = textWatcher
    }

    @Deprecated("Use stateSpinnerStatus of corresponding AddressSelectionState")
    private fun shouldShowStateSpinner() = addressViewModel.hasStatesFor(addressType)

    private fun showCountrySearchScreen(countries: List<Location>) {
        val action = OrderDetailFragmentDirections.actionSearchFilterFragment(
            items = countries.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_country_search_hint),
            requestKey = SELECT_COUNTRY_REQUEST,
            title = getString(R.string.shipping_label_edit_address_country)
        )
        findNavController().navigateSafely(action)
    }

    private fun showStateSearchScreen(states: List<Location>) {
        val action = OrderDetailFragmentDirections.actionSearchFilterFragment(
            items = states.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_state_search_hint),
            requestKey = SELECT_STATE_REQUEST,
            title = getString(R.string.shipping_label_edit_address_state)
        )
        findNavController().navigateSafely(action)
    }

    private fun setupObservers() {
        addressViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            val newCountryStatePair = new.addressSelectionStates.getValue(addressType)

            new.isLoading.takeIfNotEqualTo(old?.isLoading) {
                binding.progressBar.isVisible = it
            }
            binding.form.update(newCountryStatePair)
        }

        addressViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is AddressViewModel.ShowStateSelector -> showStateSearchScreen(event.states)
                is AddressViewModel.ShowCountrySelector -> showCountrySearchScreen(event.countries)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupResultHandlers() {
        handleResult<String>(SELECT_COUNTRY_REQUEST) {
            addressViewModel.onCountrySelected(addressType, it)
        }
        handleResult<String>(SELECT_STATE_REQUEST) {
            addressViewModel.onStateSelected(addressType, it)
        }
    }
}
