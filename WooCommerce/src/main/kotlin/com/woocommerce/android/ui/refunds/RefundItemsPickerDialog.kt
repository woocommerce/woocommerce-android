package com.woocommerce.android.ui.refunds

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.NumberPickerDialog
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

class RefundItemsPickerDialog : NumberPickerDialog(), HasSupportFragmentInjector {
    @Inject internal lateinit var childFragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) { viewModelFactory }
    private val navArgs: RefundItemsPickerDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun returnResult(selectedValue: Int) {
        viewModel.onRefundQuantityChanged(navArgs.productId, selectedValue)
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return childFragmentInjector
    }
}
