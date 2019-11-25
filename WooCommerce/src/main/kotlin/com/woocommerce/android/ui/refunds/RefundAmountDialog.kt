package com.woocommerce.android.ui.refunds

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.CurrencyAmountDialog
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.currency_amount_dialog.*
import java.math.BigDecimal
import javax.inject.Inject

class RefundAmountDialog : CurrencyAmountDialog(), HasSupportFragmentInjector {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject internal lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) { viewModelFactory }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)

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

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return childFragmentInjector
    }
}
