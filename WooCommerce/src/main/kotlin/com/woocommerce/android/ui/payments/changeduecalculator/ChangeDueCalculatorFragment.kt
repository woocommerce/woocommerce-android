package com.woocommerce.android.ui.payments.changeduecalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangeDueCalculatorFragment : BaseFragment() {

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: ChangeDueCalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewLifecycleOwnerLiveData.observe(this) { viewLifecycleOwner ->
            viewLifecycleOwner?.let { lifecycleOwner ->
                viewModel.event.observe(lifecycleOwner) { event ->
                    when (event) {
                        is MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()
                ChangeDueCalculatorScreen(
                    uiState = uiState,
                    onNavigateUp = viewModel::onBackPressed,
                    onCompleteOrderClick = {
                        extracted()
                    },
                    onAmountReceivedChanged = { viewModel.updateAmountReceived(it) }
                )
            }
        }
    }

    private fun extracted() {
        navigateBackWithResult(
            key = IS_ORDER_PAID_RESULT,
            result = true,
        )
    }

    companion object {
        const val IS_ORDER_PAID_RESULT = "is_order_paid_result"
    }
}
