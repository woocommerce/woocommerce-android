package com.woocommerce.android.ui.blaze.creation.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BlazeCampaignPaymentMethodsListFragment : BaseFragment() {
    companion object {
        const val SELECTED_PAYMENT_METHOD_KEY = "selectedPaymentMethodId"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: BlazeCampaignPaymentMethodsListViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return composeView {
            WooThemeWithBackground {
                BlazeCampaignPaymentMethodsListScreen(viewModel)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleResults()
    }

    private fun handleResults() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                is MultiLiveEvent.Event.ExitWithResult<*> -> navigateBackWithResult(
                    key = SELECTED_PAYMENT_METHOD_KEY,
                    result = event.data
                )
            }
        }
    }
}
