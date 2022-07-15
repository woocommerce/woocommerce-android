package com.woocommerce.android.ui.orders.details.customfields

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.customfields.CustomOrderFieldsHelper.CustomOrderFieldClickListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomOrderFieldsFragment : BaseFragment(), CustomOrderFieldClickListener {
    private val viewModel by hiltNavGraphViewModels<OrderDetailViewModel>(R.id.nav_graph_orders)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CustomOrderFieldsScreen(viewModel, this@CustomOrderFieldsFragment)
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_custom_fields)

    override fun onCustomOrderFieldClicked(value: String) {
        viewModel.onCustomFieldClicked(requireActivity(), value)
    }
}
