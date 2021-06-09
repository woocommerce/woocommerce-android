package com.woocommerce.android.ui.refunds

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.widgets.ConfirmationDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RefundConfirmationDialog : ConfirmationDialog() {
    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun returnResult(result: Boolean) {
        viewModel.onRefundConfirmed(result)
    }
}
