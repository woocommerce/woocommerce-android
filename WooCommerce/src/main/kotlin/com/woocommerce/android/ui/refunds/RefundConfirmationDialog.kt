package com.woocommerce.android.ui.refunds

import android.app.Dialog
import android.os.Bundle
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.ConfirmationDialog
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class RefundConfirmationDialog : ConfirmationDialog(), HasAndroidInjector {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>

    private val viewModel: IssueRefundViewModel by navGraphViewModels(R.id.nav_graph_refunds) {
        viewModelFactory.get()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        AndroidSupportInjection.inject(this)
        return super.onCreateDialog(savedInstanceState)
    }

    override fun returnResult(result: Boolean) {
        viewModel.onRefundConfirmed(result)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return childInjector
    }
}
