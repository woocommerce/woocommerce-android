package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_ONBOARDING_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_ONBOARDING_SHOWN
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_ONBOARDING_SKIP_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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

    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper

    private var prologueCarouselListener: PrologueCarouselListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLoginPrologueCarouselBinding.bind(view)
        val adapter = LoginPrologueAdapter(this)

        binding.buttonSkip.setOnClickListener {
            prologueCarouselListener?.onCarouselFinished()
            analyticsTrackerWrapper.track(LOGIN_ONBOARDING_SKIP_BUTTON_TAPPED)

            appPrefsWrapper.setOnboardingCarouselDisplayed(true)
        }

        binding.buttonNext.setOnClickListener {
            if (binding.viewPager.currentItem == adapter.itemCount - 1) {
                prologueCarouselListener?.onCarouselFinished()
                analyticsTrackerWrapper.track(
                    LOGIN_ONBOARDING_NEXT_BUTTON_TAPPED,
                    mapOf(Pair(AnalyticsTracker.VALUE_LOGIN_ONBOARDING_IS_FINAL_PAGE, true))
                )

                appPrefsWrapper.setOnboardingCarouselDisplayed(true)
            } else {
                binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
                analyticsTrackerWrapper.track(
                    LOGIN_ONBOARDING_NEXT_BUTTON_TAPPED,
                    mapOf(Pair(AnalyticsTracker.VALUE_LOGIN_ONBOARDING_IS_FINAL_PAGE, false))
                )
            }
        }

        binding.viewPager.adapter = adapter
        binding.viewPagerIndicator.setupFromViewPager(binding.viewPager)

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE_CAROUSEL)
            analyticsTrackerWrapper.track(LOGIN_ONBOARDING_SHOWN)
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
