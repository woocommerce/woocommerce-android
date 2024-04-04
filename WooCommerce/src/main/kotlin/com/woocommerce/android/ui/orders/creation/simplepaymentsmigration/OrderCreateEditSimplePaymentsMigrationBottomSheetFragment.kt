package com.woocommerce.android.ui.orders.creation.simplepaymentsmigration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class OrderCreateEditSimplePaymentsMigrationBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooTheme {
                OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
                    onAddCustomAmountClicked = { dismiss() }
                )
            }
        }
    }
}
