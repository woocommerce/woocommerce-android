package com.woocommerce.android.ui.orders.details.customfields

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.orders.details.customfields.CustomOrderFieldsHelper.CustomOrderFieldClickListener
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomOrderFieldsFragment : BaseFragment(), CustomOrderFieldClickListener {
    private val viewModel by viewModels<OrderDetailViewModel>()

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

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                MultiLiveEvent.Event.Exit -> findNavController().navigateUp()
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_custom_fields)

    override fun onCustomOrderFieldClicked(value: String) {
        viewModel.onCustomFieldClicked(requireActivity(), value)
    }
}
