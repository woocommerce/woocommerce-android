package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCreationEditCustomerAddressBinding
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.databinding.LayoutAddressSwitchBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.views.bindEditFields
import com.woocommerce.android.ui.orders.creation.views.update
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.*
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.BILLING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.SHIPPING
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationCustomerAddFragment : BaseFragment(R.layout.fragment_creation_edit_customer_address) {
    private companion object {
        const val SELECT_BILLING_COUNTRY_REQUEST = "select_billing_country_request"
        const val SELECT_BILLING_STATE_REQUEST = "select_billing_state_request"
        const val SELECT_SHIPPING_COUNTRY_REQUEST = "select_shipping_country_request"
        const val SELECT_SHIPPING_STATE_REQUEST = "select_shipping_state_request"
    }

    private val addressViewModel: AddressViewModel by viewModels()
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    private var shippingBinding: LayoutAddressFormBinding? = null
    private var billingBinding: LayoutAddressFormBinding? = null
    private var showShippingAddressFormSwitch: LayoutAddressSwitchBinding? = null
    private var doneMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        inflateLayout(view)
        setupLocationHandling()
        observeEvents()
        observeViewState()

        addressViewModel.start(
            mapOf(
                BILLING to sharedViewModel.currentDraft.billingAddress,
                SHIPPING to (
                    sharedViewModel.currentDraft.shippingAddress.takeIf {
                        it != sharedViewModel.currentDraft.billingAddress
                    } ?: Address.EMPTY
                    )
            )
        )
    }

    private fun setupLocationHandling() {
        AddressType.values().forEach {
            setupHandlingCountrySelection(it)
            setupHandlingStateSelection(it)
        }
    }

    private fun observeViewState() {
        addressViewModel.viewStateData.observe(viewLifecycleOwner) { _, new ->
            val newBilling = new.addressSelectionStates[BILLING]
            val newShipping = new.addressSelectionStates[SHIPPING]

            newBilling?.let {
                billingBinding.update(it)
            }
            if (newShipping?.address != Address.EMPTY) {
                showShippingAddressFormSwitch?.addressSwitch?.isChecked = true
                newShipping?.let {
                    shippingBinding.update(it)
                }
            }
        }
        addressViewModel.shouldShowDoneButton.observe(viewLifecycleOwner) { shouldShowDoneButton: Boolean ->
            doneMenuItem?.isVisible = shouldShowDoneButton
        }
        addressViewModel.isDifferentShippingAddressChecked.observe(viewLifecycleOwner) { checked ->
            updateShippingBindingVisibility(checked)
        }
    }

    private fun observeEvents() {
        addressViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowStateSelector -> showStateSearchScreen(event.type, event.states)
                is ShowCountrySelector -> showCountrySearchScreen(event.type, event.countries)
                is Exit -> {
                    sharedViewModel.onCustomerAddressEdited(
                        billingAddress = event.addresses.getValue(BILLING),
                        shippingAddress = event.addresses.getValue(SHIPPING)
                    )
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun inflateLayout(view: View) {
        billingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            addressSectionHeader.setText(R.string.order_detail_billing_address_section)
            countrySpinner.setClickListener {
                addressViewModel.onCountrySpinnerClicked(BILLING)
            }
            stateSpinner.setClickListener {
                addressViewModel.onStateSpinnerClicked(BILLING)
            }
        }

        shippingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            addressSectionHeader.setText(R.string.order_detail_shipping_address_section)
            email.visibility = View.GONE
            countrySpinner.setClickListener {
                addressViewModel.onCountrySpinnerClicked(SHIPPING)
            }
            stateSpinner.setClickListener {
                addressViewModel.onStateSpinnerClicked(SHIPPING)
            }
        }

        showShippingAddressFormSwitch = LayoutAddressSwitchBinding.inflate(layoutInflater)

        FragmentCreationEditCustomerAddressBinding.bind(view).container.apply {
            addView(billingBinding?.root)
            addView(showShippingAddressFormSwitch?.root)
            addView(shippingBinding?.root)
        }

        updateShippingBindingVisibility(showShippingAddressFormSwitch?.addressSwitch?.isChecked ?: false)
        showShippingAddressFormSwitch?.let {
            it.addressSwitch.setOnCheckedChangeListener { _, checked ->
                addressViewModel.onDifferentShippingAddressChecked(checked)
            }
        }

        billingBinding?.bindEditFields(
            BILLING,
            onFieldEdited = { addressType, field, value ->
                addressViewModel.onFieldEdited(addressType, field, value)
            }
        )
        shippingBinding?.bindEditFields(
            SHIPPING,
            onFieldEdited = { addressType, field, value ->
                addressViewModel.onFieldEdited(addressType, field, value)
            }
        )
    }

    private fun setupHandlingCountrySelection(addressType: AddressType) {
        handleResult<LocationCode>(
            when (addressType) {
                SHIPPING -> SELECT_SHIPPING_COUNTRY_REQUEST
                BILLING -> SELECT_BILLING_COUNTRY_REQUEST
            }
        ) { countryCode ->
            addressViewModel.onCountrySelected(addressType, countryCode)
        }
    }

    private fun setupHandlingStateSelection(addressType: AddressType) {
        handleResult<LocationCode>(
            when (addressType) {
                SHIPPING -> SELECT_SHIPPING_STATE_REQUEST
                BILLING -> SELECT_BILLING_STATE_REQUEST
            }
        ) { stateCode ->
            addressViewModel.onStateSelected(addressType, stateCode)
        }
    }

    private fun updateShippingBindingVisibility(checked: Boolean) {
        if (checked) {
            shippingBinding?.root?.visibility = View.VISIBLE
        } else {
            shippingBinding?.root?.visibility = View.GONE
        }
    }

    private fun showCountrySearchScreen(addressType: AddressType, countries: List<Location>) {
        val action = OrderCreationCustomerAddFragmentDirections.actionSearchFilterFragment(
            items = countries.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_country_search_hint),
            requestKey = when (addressType) {
                SHIPPING -> SELECT_SHIPPING_COUNTRY_REQUEST
                BILLING -> SELECT_BILLING_COUNTRY_REQUEST
            },
            title = getString(R.string.shipping_label_edit_address_country)
        )
        findNavController().navigateSafely(action)
    }

    private fun showStateSearchScreen(addressType: AddressType, states: List<Location>) {
        val action = OrderDetailFragmentDirections.actionSearchFilterFragment(
            items = states.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_state_search_hint),
            requestKey = when (addressType) {
                SHIPPING -> SELECT_SHIPPING_STATE_REQUEST
                BILLING -> SELECT_BILLING_STATE_REQUEST
            },
            title = getString(R.string.shipping_label_edit_address_state)
        )
        findNavController().navigateSafely(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done).apply {
            isVisible = addressViewModel.isAnyAddressEdited.value ?: false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                addressViewModel.onDoneSelected(
                    addDifferentShippingChecked = showShippingAddressFormSwitch?.addressSwitch?.isChecked ?: false
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_customer)

    override fun onDestroyView() {
        super.onDestroyView()
        shippingBinding = null
        billingBinding = null
        showShippingAddressFormSwitch = null
    }
}
