package com.woocommerce.android.ui.payments.refunds

import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.widgets.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RefundConfirmationDialog : ConfirmationDialog() {
    companion object {
        const val REFUND_CONFIRMATION_NOTICE = "refund_confirmation_notice"
    }

    override fun returnResult(result: Boolean) {
        if (result) {
            navigateBackWithNotice(REFUND_CONFIRMATION_NOTICE)
        } else {
            findNavController().navigateUp()
        }
    }
}
