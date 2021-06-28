package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PrintingInstructionsFragment : BaseFragment(R.layout.fragment_printing_instructions) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver
}
