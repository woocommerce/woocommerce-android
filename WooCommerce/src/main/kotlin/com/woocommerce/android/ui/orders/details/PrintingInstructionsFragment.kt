package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrintingInstructionsFragment : BaseFragment(R.layout.fragment_printing_instructions) {
    override val navigationIconForActivityToolbar: Int
        get() = R.drawable.ic_gridicons_cross_24dp
}
