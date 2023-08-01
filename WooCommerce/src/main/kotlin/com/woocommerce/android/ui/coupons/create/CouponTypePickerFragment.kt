package com.woocommerce.android.ui.coupons.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CouponTypePickerFragment : WCBottomSheetDialogFragment() {
    private val viewModel: CouponTypePickerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CouponTypePickerScreen(
                        viewModel::onPercentageDiscountClicked,
                        viewModel::onFixedCartDiscountClicked,
                        viewModel::onFixedProductDiscountClicked
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is CouponTypePickerViewModel.NavigateToCouponEdit -> {
                    findNavController().navigate(
                        CouponTypePickerFragmentDirections.actionCouponTypePickerFragmentToEditCouponFragment(
                            mode = it.mode
                        )
                    )
                }
            }
        }
    }
}
