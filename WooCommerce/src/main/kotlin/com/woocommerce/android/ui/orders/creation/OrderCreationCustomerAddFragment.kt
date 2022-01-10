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
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.views.textFieldsState
import com.woocommerce.android.ui.orders.creation.views.update
import com.woocommerce.android.ui.orders.creation.views.updateLocationStateViews
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
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

    private lateinit var doneMenuItem: MenuItem
    private lateinit var showShippingAddressFormSwitch: LayoutAddressSwitchBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        billingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            addressSectionHeader.setText(R.string.order_detail_billing_address_section)
            countrySpinner.setClickListener {
                addressViewModel.onCountrySpinnerClicked(BILLING)
            }
            stateSpinner.setClickListener {
                addressViewModel.onStateSpinnerClicked(BILLING)
            }
        }

        showShippingAddressFormSwitch = LayoutAddressSwitchBinding.inflate(layoutInflater)

        val binding = FragmentCreationEditCustomerAddressBinding.bind(view).apply {
            container.addView(billingBinding?.root)
            container.addView(showShippingAddressFormSwitch.root)
        }

        binding.updateShippingBindingVisibility(showShippingAddressFormSwitch.addressSwitch.isChecked)
        showShippingAddressFormSwitch.addressSwitch.apply {
            setOnCheckedChangeListener { _, checked ->
                binding.updateShippingBindingVisibility(checked)
            }
        }

        addressViewModel.viewStateData.observe(viewLifecycleOwner) { _, new ->
            val newBilling = new.countryStatePairs.getValue(BILLING)
            val newShipping = new.countryStatePairs.getValue(SHIPPING)

            billingBinding.update(newBilling)
            shippingBinding.update(newShipping)
        }

        AddressViewModel.AddressType.values().forEach {
            setupHandlingCountrySelection(it)
            setupHandlingStateSelection(it)
        }

        addressViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is AddressViewModel.ShowStateSelector -> showStateSearchScreen(event.type, event.states)
                is AddressViewModel.ShowCountrySelector -> showCountrySearchScreen(event.type, event.countries)
                is AddressViewModel.Exit -> {
                    sharedViewModel.onCustomerAddressEdited(event.billingAddress, event.shippingAddress)
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setupHandlingCountrySelection(addressType: AddressViewModel.AddressType) {
        handleResult<LocationCode>(
            when (addressType) {
                SHIPPING -> SELECT_SHIPPING_COUNTRY_REQUEST
                BILLING -> SELECT_BILLING_COUNTRY_REQUEST
            }
        ) { countryCode ->
            addressViewModel.onCountrySelected(addressType, countryCode)
        }
    }

    private fun setupHandlingStateSelection(addressType: AddressViewModel.AddressType) {
        handleResult<LocationCode>(
            when (addressType) {
                SHIPPING -> SELECT_SHIPPING_STATE_REQUEST
                BILLING -> SELECT_BILLING_STATE_REQUEST
            }
        ) { stateCode ->
            addressViewModel.onStateSelected(addressType, stateCode)
        }
    }

    private fun FragmentCreationEditCustomerAddressBinding.updateShippingBindingVisibility(checked: Boolean) {
        if (checked) {
            if (shippingBinding == null) {
                shippingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
                    addressSectionHeader.setText(R.string.order_detail_shipping_address_section)
                    email.visibility = View.GONE
                    countrySpinner.setClickListener {
                        addressViewModel.onCountrySpinnerClicked(SHIPPING)
                    }
                    stateSpinner.setClickListener {
                        addressViewModel.onStateSpinnerClicked(SHIPPING)
                    }
                    updateLocationStateViews(AddressViewModel.StateSpinnerStatus.DISABLED)
                }.also {
                    this.container.addView(it.root)
                }
            }
            shippingBinding?.root?.visibility = View.VISIBLE
        } else {
            shippingBinding?.root?.visibility = View.GONE
        }
    }

    private fun showCountrySearchScreen(addressType: AddressViewModel.AddressType, countries: List<Location>) {
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

    private fun showStateSearchScreen(addressType: AddressViewModel.AddressType, states: List<Location>) {
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
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = hasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                addressViewModel.onDoneSelected(
                    mapOf(
                        SHIPPING to shippingBinding.textFieldsState,
                        BILLING to billingBinding.textFieldsState
                    )
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_customer)

    override fun onDestroyView() {
        super.onDestroyView()
        addressViewModel.onViewDestroyed(
            mapOf(
                SHIPPING to shippingBinding.textFieldsState,
                BILLING to billingBinding.textFieldsState
            )
        )
        shippingBinding = null
        billingBinding = null
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun hasChanges() = true
}
