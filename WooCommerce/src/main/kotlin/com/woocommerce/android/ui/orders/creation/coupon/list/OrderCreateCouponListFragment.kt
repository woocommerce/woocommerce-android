package com.woocommerce.android.ui.orders.creation.coupon.list

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
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.coupon.list.OrderCreateCouponListFragmentDirections.Companion.actionOrderCreationCouponListFragmentToOrderCreationCouponEditFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent

class OrderCreateCouponListFragment : BaseFragment() {
    private val args: OrderCreateCouponListFragmentArgs by navArgs()
    override val activityAppBarStatus = AppBarStatus.Hidden

    private val viewModel by viewModels<OrderCreateCouponListViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                OrderCouponListScreen(
                    onNavigateBackClicked = viewModel::onNavigateBack,
                    onAddCouponClicked = viewModel::onAddCouponClicked,
                    onCouponClicked = viewModel::onCouponClicked,
                    couponsState = viewModel.coupons.observeAsState(emptyList()),
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is OrderCreateCouponListViewModel.EditCoupon -> {
                    findNavController().navigate(
                        actionOrderCreationCouponListFragmentToOrderCreationCouponEditFragment(
                            args.orderCreationMode,
                            it.couponCode,
                            args.orderDraft
                        )
                    )
                }

                is OrderCreateCouponListViewModel.AddCoupon -> {
                    findNavController().navigate(
                        actionOrderCreationCouponListFragmentToOrderCreationCouponEditFragment(
                            args.orderCreationMode,
                            orderDraft = args.orderDraft
                        )
                    )
                }

                is MultiLiveEvent.Event.Exit -> {
                    findNavController().popBackStack()
                }
            }
        }
    }
}
