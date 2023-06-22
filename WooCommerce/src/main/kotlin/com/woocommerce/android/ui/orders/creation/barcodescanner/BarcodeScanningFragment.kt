package com.woocommerce.android.ui.orders.creation.barcodescanner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment

class BarcodeScanningFragment : BaseFragment(R.layout.fragment_barcode_scanning) {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
