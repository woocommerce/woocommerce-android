package com.woocommerce.android.support.requests

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SupportRequestFormFragment : BaseFragment(R.layout.fragment_support_request_form) {
    private val viewModel: SupportRequestFormViewModel by viewModels()
}
