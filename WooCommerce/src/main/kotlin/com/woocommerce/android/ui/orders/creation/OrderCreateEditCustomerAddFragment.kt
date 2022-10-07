package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditCustomerAddressBinding
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.databinding.LayoutAddressSwitchBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.creation.views.bindEditFields
import com.woocommerce.android.ui.orders.creation.views.update
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.BILLING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.SHIPPING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.Exit
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ShowCountrySelector
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ShowStateSelector
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditCustomerAddFragment :
    BaseFragment(R.layout.fragment_order_create_edit_customer_address),
    MenuProvider {
    private companion object {
        const val SELECT_BILLING_COUNTRY_REQUEST = "select_billing_country_request"
        const val SELECT_BILLING_STATE_REQUEST = "select_billing_state_request"
        const val SELECT_SHIPPING_COUNTRY_REQUEST = "select_shipping_country_request"
        const val SELECT_SHIPPING_STATE_REQUEST = "select_shipping_state_request"
        private const val SEARCH_ID = Int.MAX_VALUE
    }

    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)
    private val addressViewModel by hiltNavGraphViewModels<AddressViewModel>(R.id.nav_graph_order_creations)

    private var fragmentViewBinding: FragmentOrderCreateEditCustomerAddressBinding? = null
    private var shippingBinding: LayoutAddressFormBinding? = null
    private var billingBinding: LayoutAddressFormBinding? = null
    private var showShippingAddressFormSwitch: LayoutAddressSwitchBinding? = null
    private var doneMenuItem: MenuItem? = null

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
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
            fragmentViewBinding?.progressIndicator?.isVisible = new.isLoading

            val newBilling = new.addressSelectionStates[BILLING]
            val newShipping = new.addressSelectionStates[SHIPPING]

            newBilling?.let {
                billingBinding.update(it)
            }
            newShipping?.let {
                shippingBinding.update(it)
            }

            if (newShipping?.address != Address.EMPTY) {
                showShippingAddressFormSwitch?.addressSwitch?.isChecked = true
            }
        }
        addressViewModel.shouldEnableDoneButton.observe(viewLifecycleOwner) { shouldShowDoneButton: Boolean ->
            doneMenuItem?.isEnabled = shouldShowDoneButton
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
                is MultiLiveEvent.Event.ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> {
                    sharedViewModel.onCustomerAddressEdited(
                        billingAddress = event.addresses.getValue(BILLING),
                        shippingAddress = event.addresses.getValue(SHIPPING)
                    )
                    findNavController().navigateUp()
                }
                is AddressViewModel.SearchCustomers -> showCustomerSearchScreen()
            }
        }
    }

    private fun inflateLayout(view: View) {
        billingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            setAccessibilityHeaders(R.string.details, R.string.order_detail_billing_address_section)

            countrySpinner.setClickListener {
                addressViewModel.onCountrySpinnerClicked(BILLING)
            }
            stateSpinner.setClickListener {
                addressViewModel.onStateSpinnerClicked(BILLING)
            }
        }

        shippingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            setAccessibilityHeaders(R.string.details, R.string.order_detail_shipping_address_section)

            email.visibility = View.GONE
            countrySpinner.setClickListener {
                addressViewModel.onCountrySpinnerClicked(SHIPPING)
            }
            stateSpinner.setClickListener {
                addressViewModel.onStateSpinnerClicked(SHIPPING)
            }
        }

        showShippingAddressFormSwitch = LayoutAddressSwitchBinding.inflate(layoutInflater)

        fragmentViewBinding = FragmentOrderCreateEditCustomerAddressBinding.bind(view)
        fragmentViewBinding?.container.apply {
            if (this != null) {
                addView(billingBinding?.root)
                addView(showShippingAddressFormSwitch?.root)
                addView(shippingBinding?.root)
            }
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

    private fun LayoutAddressFormBinding.setAccessibilityHeaders(detailsHeading: Int, addressHeading: Int) {
        detailsHeaderContainer.announceForAccessibility(detailsHeading.toString())
        ViewCompat.setAccessibilityHeading(detailsHeaderContainer, true)

        addressHeaderContainer.announceForAccessibility(addressHeading.toString())
        addressSectionHeader.setText(addressHeading)
        ViewCompat.setAccessibilityHeading(addressHeaderContainer, true)
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
        val action = OrderCreateEditCustomerAddFragmentDirections.actionSearchFilterFragment(
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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()

        if (FeatureFlag.ORDER_CREATION_CUSTOMER_SEARCH.isEnabled()) {
            menu.add(
                Menu.NONE,
                SEARCH_ID,
                Menu.NONE,
                android.R.string.search_go
            ).also {
                it.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                it.setIcon(R.drawable.ic_search_24dp)
            }
        }

        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done).apply {
            isEnabled = addressViewModel.shouldEnableDoneButton.value ?: false
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                addressViewModel.onDoneSelected(
                    addDifferentShippingChecked = showShippingAddressFormSwitch?.addressSwitch?.isChecked ?: false
                )
                true
            }
            SEARCH_ID -> {
                addressViewModel.onCustomerSearchClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_customer_details)

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentViewBinding = null
        shippingBinding = null
        billingBinding = null
        showShippingAddressFormSwitch = null
    }

    private fun showCustomerSearchScreen() {
        findNavController().navigateSafely(
            OrderCreateEditCustomerAddFragmentDirections.actionGlobalCustomerListFragment()
        )
    }
}
