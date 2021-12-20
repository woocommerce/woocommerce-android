package com.woocommerce.android.ui.orders.creation

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCreationEditCustomerAddressBinding
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationCustomerAddFragment : BaseFragment(R.layout.fragment_creation_edit_customer_address) {
    companion object {
        const val SELECT_BILLING_COUNTRY_REQUEST = "select_billing_country_request"
        const val SELECT_BILLING_STATE_REQUEST = "select_billing_state_request"
    }

    private val addressViewModel: AddressViewModel by viewModels()

    private var _binding: FragmentCreationEditCustomerAddressBinding? = null

    private var _billingBinding: LayoutAddressFormBinding? = null
    private val billingBinding
        get() = _billingBinding!!

    private lateinit var doneMenuItem: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _billingBinding = LayoutAddressFormBinding.inflate(layoutInflater).apply {
            addressSectionHeader.setText(R.string.order_detail_billing_address_section)
            countrySpinner.setClickListener {
                showCountrySearchScreen()
            }
            stateSpinner.setClickListener {
                showStateSearchScreen(AddressViewModel.AddressType.BILLING)
            }
        }

        _binding = FragmentCreationEditCustomerAddressBinding.bind(view).apply {
            container.addView(billingBinding.root)
        }

        addressViewModel.viewStateData.observe(viewLifecycleOwner) { _, new ->
            val newCountryStatePair = new.countryStatePairs[AddressViewModel.AddressType.BILLING]

            billingBinding.countrySpinner.setText(newCountryStatePair?.countryLocation?.name.orEmpty())
            billingBinding.stateSpinner.setText(newCountryStatePair?.stateLocation?.name.orEmpty())
            updateStateViews(billingBinding, AddressViewModel.AddressType.BILLING)
        }

        handleResult<String>(SELECT_BILLING_COUNTRY_REQUEST) {
            addressViewModel.onCountrySelected(AddressViewModel.AddressType.BILLING, it)
        }
        handleResult<String>(SELECT_BILLING_STATE_REQUEST) {
            addressViewModel.onStateSelected(AddressViewModel.AddressType.BILLING, it)
        }
    }

    private fun showCountrySearchScreen() {
        val countries = addressViewModel.countries
        val action = OrderCreationCustomerAddFragmentDirections.actionSearchFilterFragment(
            items = countries.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_country_search_hint),
            requestKey = SELECT_BILLING_COUNTRY_REQUEST,
            title = getString(R.string.shipping_label_edit_address_country)
        )
        findNavController().navigateSafely(action)
    }

    private fun showStateSearchScreen(addressType: AddressViewModel.AddressType) {
        val states = addressViewModel.statesAvailableFor(addressType)
        val action = OrderDetailFragmentDirections.actionSearchFilterFragment(
            items = states.map {
                SearchFilterItem(
                    name = it.name,
                    value = it.code
                )
            }.toTypedArray(),
            hint = getString(R.string.shipping_label_edit_address_state_search_hint),
            requestKey = SELECT_BILLING_STATE_REQUEST,
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
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_new_customer)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _billingBinding = null
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun hasChanges() = true

    private fun shouldShowStateSpinnerFor(addressType: AddressViewModel.AddressType) =
        addressViewModel.hasStatesFor(addressType)

    private fun updateStateViews(binding: LayoutAddressFormBinding, addressType: AddressViewModel.AddressType) {
        binding.stateSpinner.isVisible = shouldShowStateSpinnerFor(addressType)
        binding.stateEditText.isVisible = !shouldShowStateSpinnerFor(addressType)
    }
}
