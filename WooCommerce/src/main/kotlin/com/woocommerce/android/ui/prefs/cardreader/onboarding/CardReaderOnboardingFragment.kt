package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCardReaderOnboardingBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderOnboardingFragment : BaseFragment(R.layout.fragment_card_reader_onboarding) {
    val viewModel: CardReaderOnboardingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCardReaderOnboardingBinding.bind(view)
        initObservers(binding)
    }

    private fun initObservers(binding: FragmentCardReaderOnboardingBinding) {
        viewModel.event.observe(
            viewLifecycleOwner,
            { event ->
                when (event) {
                    is MultiLiveEvent.Event.Exit -> navigateBackWithNotice(KEY_READER_ONBOARDING_RESULT)
                    else -> event.isHandled = false
                }
            }
        )

        viewModel.viewStateData.observe(
            viewLifecycleOwner,
            { state ->
                showOnboardingLayout(binding, state.layoutRes)
            }
        )
    }

    private fun showOnboardingLayout(binding: FragmentCardReaderOnboardingBinding, @LayoutRes layoutRes: Int) {
        binding.container.removeAllViews()
        LayoutInflater.from(requireActivity()).inflate(layoutRes, binding.container, true)
    }

    override fun getFragmentTitle() = resources.getString(R.string.payment_onboarding_title)

    companion object {
        const val KEY_READER_ONBOARDING_RESULT = "key_reader_onboarding_result"
    }
}
