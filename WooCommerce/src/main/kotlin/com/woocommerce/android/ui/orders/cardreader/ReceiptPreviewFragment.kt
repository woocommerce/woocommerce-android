package com.woocommerce.android.ui.orders.cardreader

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentReceiptPreviewBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReceiptPreviewFragment: BaseFragment(R.layout.fragment_receipt_preview) {
    val viewModel: ReceiptPreviewViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentReceiptPreviewBinding.bind(view)
    }
}
