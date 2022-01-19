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
import com.woocommerce.android.ui.orders.creation.views.update
import com.woocommerce.android.ui.orders.creation.views.updateLocationStateViews
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.*
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.BILLING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.SHIPPING
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
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

    private var billingBinding: LayoutAddressFormBinding? = null
    private var shippingBinding: LayoutAddressFormBinding? = null

    private lateinit var doneMenuItem: MenuItem
    private lateinit var showShippingAddressFormSwitch: LayoutAddressSwitchBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        billingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            billingBinding?.firstName?.isSaveEnabled = false
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
            updateLocationStateViews(StateSpinnerStatus.DISABLED)
            root.visibility = View.GONE
        }

        showShippingAddressFormSwitch = LayoutAddressSwitchBinding.inflate(layoutInflater)

        FragmentCreationEditCustomerAddressBinding.bind(view).apply {
            container.addView(billingBinding?.root)
            container.addView(showShippingAddressFormSwitch.root)
            container.addView(shippingBinding?.root)
        }

        addressViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            val newBilling = new.countryStatePairs[BILLING]
            val newShipping = new.countryStatePairs[SHIPPING]

            newBilling?.let {
                billingBinding.update(it)
            }
            if (newShipping?.address != Address.EMPTY) {
                showShippingAddressFormSwitch.addressSwitch.isChecked = true
                newShipping?.let {
                    shippingBinding.update(it)
                }
            }
        }

        updateShippingBindingVisibility(showShippingAddressFormSwitch.addressSwitch.isChecked)
        showShippingAddressFormSwitch.addressSwitch.apply {
            setOnCheckedChangeListener { _, checked ->
                updateShippingBindingVisibility(checked)
            }
        }

        AddressType.values().forEach {
            setupHandlingCountrySelection(it)
            setupHandlingStateSelection(it)
        }

        billingBinding?.foo(BILLING)
        shippingBinding?.foo(SHIPPING)

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

        addressViewModel.start(
            mapOf(
                BILLING to sharedViewModel.currentDraft.billingAddress,
                SHIPPING to sharedViewModel.currentDraft.shippingAddress
            )
        )
    }

    private fun WCMaterialOutlinedEditTextView.bindToField(addressType: AddressType, field: Field) {
        setOnTextChangedListener {
            addressViewModel.onFieldEdited(addressType, field, it?.toString().orEmpty())
        }
    }

    private fun LayoutAddressFormBinding.foo(addressType: AddressType) {
        this.firstName.bindToField(addressType, Field.FirstName)
        this.lastName.bindToField(addressType, Field.LastName)
        this.company.bindToField(addressType, Field.Company)
        this.address1.bindToField(addressType, Field.Address1)
        this.address2.bindToField(addressType, Field.Address2)
        this.phone.bindToField(addressType, Field.Phone)
        this.city.bindToField(addressType, Field.City)
        this.postcode.bindToField(addressType, Field.Zip)
        this.stateEditText.bindToField(addressType, Field.State)
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
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = hasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                addressViewModel.onDoneSelected()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_customer)

    override fun onDetach() {
        addressViewModel.onScreenDetached()
        super.onDetach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        shippingBinding = null
        billingBinding = null
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun hasChanges() = true
}
