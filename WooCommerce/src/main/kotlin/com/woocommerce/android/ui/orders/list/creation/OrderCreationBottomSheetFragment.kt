package com.woocommerce.android.ui.orders.list.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.databinding.DialogOrderCreationBottomSheetBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.orders.list.creation.OrderCreationBottomSheetFragment.CreationAction.CREATE_ORDER
import com.woocommerce.android.ui.orders.list.creation.OrderCreationBottomSheetFragment.CreationAction.SIMPLE_PAYMENT

class OrderCreationBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        const val KEY_ORDER_CREATION_ACTION_RESULT = "key_order_creation_action_result"
    }

    private var _binding: DialogOrderCreationBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogOrderCreationBottomSheetBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.orderCreationButton.setOnClickListener {
            navigateBackWithResult(KEY_ORDER_CREATION_ACTION_RESULT, CREATE_ORDER)
        }

        binding.simplePaymentButton.setOnClickListener {
            navigateBackWithResult(KEY_ORDER_CREATION_ACTION_RESULT, SIMPLE_PAYMENT)
        }
    }

    enum class CreationAction {
        CREATE_ORDER,
        SIMPLE_PAYMENT
    }
}
