package com.woocommerce.android.ui.orders.creation.simplepaymentsmigration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class OrderCreateEditSimplePaymentsMigrationBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_ON_ADD_CUSTOM_AMOUNT_CLICKED_NOTICE = "on_add_custom_amount_clicked_notice"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooTheme {
                OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
                    onAddCustomAmountClicked = {
                        navigateBackWithNotice(key = KEY_ON_ADD_CUSTOM_AMOUNT_CLICKED_NOTICE)
                    }
                )
            }
        }
    }
}
