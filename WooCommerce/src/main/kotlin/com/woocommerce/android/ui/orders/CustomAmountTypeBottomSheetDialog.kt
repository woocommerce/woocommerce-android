package com.woocommerce.android.ui.orders

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class CustomAmountTypeBottomSheetDialog : WCBottomSheetDialogFragment() {
    private val sharedViewModel: OrderCreateEditViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_order_creations)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CustomAmountTypeBottomSheet(sharedViewModel.getCurrencySymbol()) { customAmountType ->
                        sharedViewModel.onCustomAmountTypeSelected(customAmountType)
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        sharedViewModel.clearSelectedCustomAmount()
    }
}
