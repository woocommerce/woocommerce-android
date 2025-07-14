package com.woocommerce.android.ui.coupons.selector

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CouponSelectorFragment : BaseFragment() {
    private val viewModel by viewModels<CouponSelectorViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CouponSelectorScreen(
                        state = viewModel.couponSelectorState.observeAsState(),
                        onCouponClicked = viewModel::onCouponClicked,
                        onRefresh = viewModel::onRefresh,
                        onLoadMore = viewModel::onLoadMore,
                        onEmptyScreenButtonClicked = viewModel::onEmptyScreenButtonClicked,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is MultiLiveEvent.Event.ExitWithResult<*> -> {
                    navigateBackWithResult(KEY_COUPON_SELECTOR_RESULT, it.data)
                }

                is NavigateToCouponList -> {
                    findNavController().navigateSafely(
                        CouponSelectorFragmentDirections.actionCouponSelectorFragmentToCouponListFragment()
                    )
                }
            }
        }
    }

    companion object {
        const val KEY_COUPON_SELECTOR_RESULT = "coupon-selector-result"
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_select_coupon)
}
