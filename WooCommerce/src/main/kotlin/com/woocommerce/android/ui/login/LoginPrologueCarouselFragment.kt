package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueCarouselBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginPrologueCarouselFragment : Fragment(R.layout.fragment_login_prologue_carousel) {
    companion object {
        const val TAG = "login-prologue-carousel-fragment"

        fun newInstance(): LoginPrologueCarouselFragment {
            return LoginPrologueCarouselFragment()
        }
    }

    interface PrologueCarouselListener {
        fun onCarouselFinished()
    }

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    private var prologueCarouselListener: PrologueCarouselListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLoginPrologueCarouselBinding.bind(view)
        val adapter = LoginPrologueAdapter(this)

        binding.buttonNext.setOnClickListener {
            if (binding.viewPager.currentItem == adapter.itemCount - 1) {
                prologueCarouselListener?.onCarouselFinished()
            } else {
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
            }
        }

        binding.buttonSkip.setOnClickListener {
            prologueCarouselListener?.onCarouselFinished()
        }

        binding.viewPager.adapter = adapter
        binding.viewPagerIndicator.setupFromViewPager(binding.viewPager)

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE_CAROUSEL)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE_CAROUSEL)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is PrologueCarouselListener) {
            prologueCarouselListener = activity as PrologueCarouselListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        prologueCarouselListener = null
    }
}
