package com.woocommerce.android.ui.orders.creation.customerlistnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerListFragment : BaseFragment() {
    private val viewModel by viewModels<CustomerListViewModel>()
    private val addressViewModel by hiltNavGraphViewModels<AddressViewModel>(R.id.nav_graph_order_creations)

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                CustomerListScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is CustomerSelected -> {
                    addressViewModel.onAddressesChanged(
                        customerId = event.customerId,
                        billingAddress = event.billingAddress,
                        shippingAddress = event.shippingAddress
                    )
                    findNavController().navigateSafely(
                        CustomerListFragmentDirections
                            .actionCustomerListFragmentToOrderCreationCustomerFragment()
                    )
                }
                is AddCustomer -> {
                    addressViewModel.onAddressesChanged(
                        customerId = 0,
                        billingAddress = Address.EMPTY,
                        shippingAddress = Address.EMPTY,
                    )
                    findNavController().navigateSafely(
                        CustomerListFragmentDirections
                            .actionCustomerListFragmentToOrderCreationCustomerFragment()
                    )
                }
                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }
}
