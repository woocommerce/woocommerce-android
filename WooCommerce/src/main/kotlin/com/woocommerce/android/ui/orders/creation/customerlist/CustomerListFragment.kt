package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerListFragment : BaseFragment() {
    companion object {
        const val KEY_CUSTOMER_ID_RESULT = "customer_id"
    }

    private val viewModel by viewModels<CustomerListViewModel>()
    private val addressViewModel by fixedHiltNavGraphViewModels<AddressViewModel>(R.id.nav_graph_order_creations)
    private val sharedViewModel by fixedHiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

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
                    sharedViewModel.onCustomerEdited(event.customer)
                    addressViewModel.onAddressesChanged(event.customer)

//                    findNavController().popBackStack(R.id.orderCreationFragment, false)
                    navigateBackWithResult(
                        KEY_CUSTOMER_ID_RESULT,
                        event.customer
                    )
                }

                is AddCustomer -> {
                    addressViewModel.clearSelectedAddress()
                    addressViewModel.onFieldEdited(
                        AddressViewModel.AddressType.BILLING,
                        AddressViewModel.Field.Email,
                        event.email.orEmpty(),
                    )

                    findNavController().navigateSafely(
                        CustomerListFragmentDirections
                            .actionCustomerListFragmentToOrderCreationCustomerFragment(
                                editingOfAddedCustomer = false
                            )
                    )
                }

                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
