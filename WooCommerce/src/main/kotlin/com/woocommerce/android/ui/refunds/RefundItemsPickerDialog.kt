package com.woocommerce.android.ui.refunds

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.widgets.NumberPickerDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RefundItemsPickerDialog : NumberPickerDialog() {
    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    private val navArgs: RefundItemsPickerDialogArgs by navArgs()

    override fun returnResult(selectedValue: Int) {
        viewModel.onRefundQuantityChanged(navArgs.uniqueId, selectedValue)
    }
}
