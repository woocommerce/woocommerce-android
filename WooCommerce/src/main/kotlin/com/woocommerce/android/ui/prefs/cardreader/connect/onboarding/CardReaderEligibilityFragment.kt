package com.woocommerce.android.ui.prefs.cardreader.connect.onboarding

import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderEligibilityFragment : BaseFragment(R.layout.fragment_card_reader_eligibility) {
    val viewModel: CardReaderEligibilityViewModel by viewModels()
}
