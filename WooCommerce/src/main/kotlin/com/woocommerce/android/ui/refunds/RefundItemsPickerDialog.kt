package com.woocommerce.android.ui.refunds

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.widgets.NumberPickerDialog
import dagger.android.DispatchingAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RefundItemsPickerDialog : NumberPickerDialog() {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    private val navArgs: RefundItemsPickerDialogArgs by navArgs()

    override fun returnResult(selectedValue: Int) {
        viewModel.onRefundQuantityChanged(navArgs.uniqueId, selectedValue)
    }
}
