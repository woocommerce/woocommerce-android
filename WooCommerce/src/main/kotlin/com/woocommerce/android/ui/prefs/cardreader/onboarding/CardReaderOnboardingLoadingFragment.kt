package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingLoadingBinding

class CardReaderOnboardingLoadingFragment : BaseOnboardingFragment(R.layout.fragment_card_reader_onboarding_loading) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderOnboardingLoadingBinding.bind(view)
        illustration = binding.illustration

        binding.cancelButton.setOnClickListener {
            onCancel()
        }
    }

    companion object {
        fun newInstance(): CardReaderOnboardingLoadingFragment = CardReaderOnboardingLoadingFragment()
    }
}
