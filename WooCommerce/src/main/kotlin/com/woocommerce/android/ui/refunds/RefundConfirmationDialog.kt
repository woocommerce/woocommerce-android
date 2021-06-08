package com.woocommerce.android.ui.refunds

import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.widgets.ConfirmationDialog
import dagger.android.DispatchingAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RefundConfirmationDialog : ConfirmationDialog() {
    @Inject internal lateinit var childInjector: DispatchingAndroidInjector<Any>

    private val viewModel: IssueRefundViewModel by hiltNavGraphViewModels(R.id.nav_graph_refunds)

    override fun returnResult(result: Boolean) {
        viewModel.onRefundConfirmed(result)
    }
}
