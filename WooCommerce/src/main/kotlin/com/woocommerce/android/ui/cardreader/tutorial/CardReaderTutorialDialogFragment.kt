package com.woocommerce.android.ui.cardreader.tutorial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.widget.ViewPager2
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.CardReaderTutorialDialogBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.cardreader.onboarding.CardReaderFlowParam
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CardReaderTutorialDialogFragment : DialogFragment(R.layout.card_reader_tutorial_dialog) {
    private val args: CardReaderTutorialDialogFragmentArgs by navArgs()
    @Inject lateinit var appPrefs: AppPrefs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dialog?.let {
            it.setCancelable(false)
            it.setCanceledOnTouchOutside(false)
            it.requestWindowFeature(Window.FEATURE_NO_TITLE)
        }

        if (!appPrefs.getShowCardReaderConnectedTutorial()) {
            navigateNext()
            dismiss()
            return null
        } else {
            appPrefs.setShowCardReaderConnectedTutorial(false)
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = CardReaderTutorialDialogBinding.bind(view)

        binding.viewPager.adapter = CardReaderTutorialAdapter(this)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val lastPosition = CardReaderTutorialAdapter.NUM_PAGES - 1
                binding.buttonSkip.setText(if (position == lastPosition) R.string.close else R.string.skip)
            }
        })
        binding.viewPagerIndicator.setupFromViewPager(binding.viewPager)

        binding.buttonSkip.setOnClickListener { navigateNext() }
    }

    private fun navigateNext() {
        when (val param = args.cardReaderFlowParam) {
            CardReaderFlowParam.CardReadersHub -> findNavController().popBackStack()
            is CardReaderFlowParam.PaymentOrRefund -> {
                val action = CardReaderTutorialDialogFragmentDirections
                    .actionCardReaderTutorialDialogFragmentToCardReaderPaymentDialogFragment(param)
                findNavController().navigateSafely(action, skipThrottling = true)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
