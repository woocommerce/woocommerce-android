package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateCouponSelectorFragment : BaseFragment() {
//    private val viewModel by viewModels<OrderCreateCouponSelectorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    OrderCreateCouponSelectorScreen(
//                        viewModel = viewModel,
//                        onCouponSelected = { /*TODO*/ }
                    )
                }
            }
        }
    }
}
