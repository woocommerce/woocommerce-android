package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.edgeToEdgeForInLandscape
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerListDialogFragment : DialogFragment() {
    companion object {
        const val KEY_CUSTOMER_RESULT = "customer_model"
    }

    private val viewModel by viewModels<CustomerListSelectionViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Slide)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        edgeToEdgeForInLandscape()
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                CustomerListSelectionScreen(viewModel = viewModel, handleInsets = true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is CustomerSelected -> {
                    navigateBackWithResult(
                        KEY_CUSTOMER_RESULT,
                        event.customer
                    )
                }

                is AddCustomer -> {
                    findNavController().navigateSafely(
                        OrderCustomerListFragmentDirections
                            .actionCustomerListFragmentToOrderCreationCustomerFragment(
                                editingOfAddedCustomer = false,
                                initialEmail = event.email.orEmpty()
                            )
                    )
                }

                is MultiLiveEvent.Event.ShowDialog -> event.showIn(requireActivity())

                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
