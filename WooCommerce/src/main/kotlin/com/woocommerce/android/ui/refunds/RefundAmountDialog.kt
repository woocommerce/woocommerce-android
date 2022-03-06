package com.woocommerce.android.ui.refunds

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.CurrencyAmountDialog
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class RefundAmountDialog : CurrencyAmountDialog() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun returnResult(enteredAmount: BigDecimal) {
        viewModel.onProductsRefundAmountChanged(enteredAmount)
    }
}
