package com.woocommerce.android.ui.orders.wooshippinglabels.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooShippingEditPaymentDialogFragment : WCBottomSheetDialogFragment() {
    private val viewModel: WooShippingEditPaymentViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return composeView {
            WooShippingEditPaymentScreen(
                viewModel = viewModel
            )
        }
    }
}
