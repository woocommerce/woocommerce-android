package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogSimplePaymentsMovedBottomSheetBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.orders.list.OrderCreationBottomSheetFragment.OrderCreationAction
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class SimplePaymentsMovedBottomSheetFragment : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return DialogSimplePaymentsMovedBottomSheetBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DialogSimplePaymentsMovedBottomSheetBinding.bind(view).apply {
            simplePaymentsMovedBtn.setOnClickListener {
                navigateBackWithResult(
                    OrderCreationBottomSheetFragment.KEY_ORDER_CREATION_ACTION_RESULT,
                    OrderCreationAction.SIMPLE_PAYMENT,
                    R.id.orders
                )
            }
        }
    }
}
