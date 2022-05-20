package com.woocommerce.android.ui.orders.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.databinding.DialogOrderCreationBottomSheetBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.orders.list.OrderCreationBottomSheetFragment.OrderCreationAction.CREATE_ORDER
import com.woocommerce.android.ui.orders.list.OrderCreationBottomSheetFragment.OrderCreationAction.SIMPLE_PAYMENT
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class OrderCreationBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_ORDER_CREATION_ACTION_RESULT = "key_order_creation_action_result"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return DialogOrderCreationBottomSheetBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogOrderCreationBottomSheetBinding.bind(view)

        binding.orderCreationButton.setOnClickListener {
            navigateBackWithResult(KEY_ORDER_CREATION_ACTION_RESULT, CREATE_ORDER)
        }

        binding.simplePaymentButton.setOnClickListener {
            navigateBackWithResult(KEY_ORDER_CREATION_ACTION_RESULT, SIMPLE_PAYMENT)
        }
    }

    enum class OrderCreationAction {
        CREATE_ORDER,
        SIMPLE_PAYMENT
    }
}
