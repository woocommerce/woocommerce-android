package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogSimplePaymentsMovedBottomSheetBinding
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class SimplePaymentsMovedBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return DialogSimplePaymentsMovedBottomSheetBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DialogSimplePaymentsMovedBottomSheetBinding.bind(view).apply {
            simplePaymentsMovedBtn.setOnClickListener {
                AnalyticsTracker.track(AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_STARTED)
                // Temporary flow to teach merchants about placement of the Payments feature
                (requireActivity() as MainNavigationRouter).showMoreMenu()
                dismiss()
            }
        }
    }
}
