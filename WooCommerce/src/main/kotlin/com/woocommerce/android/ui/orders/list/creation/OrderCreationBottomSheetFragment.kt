package com.woocommerce.android.ui.orders.list.creation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.databinding.DialogOrderCreationBottomSheetBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderCreationBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: DialogOrderCreationBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogOrderCreationBottomSheetBinding.inflate(inflater)
        return binding.root
    }
}
