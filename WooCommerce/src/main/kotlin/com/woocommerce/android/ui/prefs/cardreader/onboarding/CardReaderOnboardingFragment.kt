package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingBinding
import com.woocommerce.android.ui.base.BaseFragment

class CardReaderOnboardingFragment : BaseFragment(R.layout.fragment_card_reader_onboarding) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCardReaderOnboardingBinding.bind(view)
        showOnboardingFragment(binding)
    }

    private fun showOnboardingFragment(binding: FragmentCardReaderOnboardingBinding) {
        // TODO: this is hard-coded for now but will be changed as we add more onboarding fragments
        // to navigate to the desired fragment
        val fragment = CardReaderOnboardingLoadingFragment.newInstance()
        parentFragmentManager.beginTransaction()
            .replace(binding.container.id, fragment)
            .setCustomAnimations(
                R.anim.activity_slide_in_from_right,
                R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left,
                R.anim.activity_slide_out_to_right
            )
            .commitAllowingStateLoss()
    }

    override fun getFragmentTitle() = resources.getString(R.string.payment_onboarding_title)
}
