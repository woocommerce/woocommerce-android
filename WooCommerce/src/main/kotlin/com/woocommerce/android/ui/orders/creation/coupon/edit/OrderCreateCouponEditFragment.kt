package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponEditFragmentDirections.Companion.actionOrderCreationCouponEditFragmentToOrderCreationFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreateCouponEditFragment : BaseFragment() {
    private val args: OrderCreateCouponEditFragmentArgs by navArgs()
    private val viewModel by viewModels<OrderCreateCouponDetailsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state = viewModel.viewState.observeAsState()
                WooThemeWithBackground {
                    OrderCreateCouponEditScreen(
                        state = state,
                        onCouponRemoved = viewModel::onCouponRemoved
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon -> {
                    val action = actionOrderCreationCouponEditFragmentToOrderCreationFragment(
                        mode = args.orderCreationMode,
                        couponEditResult = OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon(
                            it.couponCode
                        ),
                        sku = null,
                        barcodeFormat = null
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_remove_this_coupon)
}
