package com.woocommerce.android.ui.orders.creation.coupon.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
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
                    navigateBackWithResult(
                        key = KEY_COUPON_EDIT_RESULT,
                        result = it,
                        destinationId = R.id.orderCreationFragment
                    )
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_remove_this_coupon)

    companion object {
        const val KEY_COUPON_EDIT_RESULT = "key_coupon_edit_result"
    }
}
