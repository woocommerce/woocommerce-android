package com.woocommerce.android.ui.refunds

import android.app.Dialog
import android.os.Bundle
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.CurrencyAmountDialog
import dagger.hilt.android.AndroidEntryPoint
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class RefundAmountDialog : CurrencyAmountDialog() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel.productsRefundLiveData.observe(this) { old, new ->
            new.takeIfNotEqualTo(old?.currency) {
                initializeCurrencyEditText(new.currency ?: "", new.decimals, currencyFormatter)
            }
        }

        return super.onCreateDialog(savedInstanceState)
    }

    override fun returnResult(enteredAmount: BigDecimal) {
        viewModel.onProductsRefundAmountChanged(enteredAmount)
    }
}
