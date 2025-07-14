package com.woocommerce.android.ui.orders.wooshippinglabels.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import com.woocommerce.android.ui.searchfilter.SearchFilterItem
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooShippingEditAddressFragment : BaseFragment(), BackPressListener {
    companion object {
        private const val SELECT_COUNTRY_REQUEST = "select_address_country_request"
        private const val SELECT_STATE_REQUEST = "select_address_state_request"
        const val DESTINATION_ADDRESS_UPDATE_RESULT = "destination_address_update_result"
    }

    private val viewModel: WooShippingEditAddressViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    Surface {
                        WooShippingEditAddressScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupHandlingResults()
        observeEvents()
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is WooShippingEditAddressViewModel.ShowCountrySelector -> showCountrySearchScreen(event.countries)
                is WooShippingEditAddressViewModel.ShowStateSelector -> showStatesSearchScreen(event.states)
                is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(
                    key = DESTINATION_ADDRESS_UPDATE_RESULT,
                    result = event.data
                )
            }
        }
    }

    private fun setupHandlingResults() {
        handleResult<LocationCode>(SELECT_COUNTRY_REQUEST) { countryCode ->
            viewModel.onCountryChanged(countryCode)
        }
        handleResult<LocationCode>(SELECT_STATE_REQUEST) { stateCode ->
            viewModel.onStateChanged(stateCode)
        }
    }

    private fun showCountrySearchScreen(countries: List<Location>) {
        val action = WooShippingEditAddressFragmentDirections.Companion.actionSearchFilterFragment(
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

    private fun showStatesSearchScreen(states: List<Location>) {
        val action = WooShippingEditAddressFragmentDirections.Companion.actionSearchFilterFragment(
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

    override fun onRequestAllowBackPress(): Boolean = viewModel.handleBackPress()
}
