package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject
import kotlin.math.roundToInt

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

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private var prologueCarouselListener: PrologueCarouselListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? DynamicEdgeToEdgeActivity)?.enableDynamicEdgeToEdge()

        val binding = FragmentLoginPrologueCarouselBinding.bind(view)

        val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.buttonSkip.updateLayoutParams<MarginLayoutParams> {
                val currentBottomMargin = resources.getDimension(R.dimen.prologue_button_skip_bottom_margin)
                bottomMargin = currentBottomMargin.roundToInt() + insets.bottom
                if (!isTablet) {
                    val buttonHorizontalMargin = resources.getDimension(R.dimen.prologue_button_horizontal_margin)
                    rightMargin = buttonHorizontalMargin.roundToInt() + insets.right
                }
            }
            WindowInsetsCompat.CONSUMED
        }
        if (!isTablet) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.buttonNext) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val buttonHorizontalMargin = resources.getDimension(R.dimen.prologue_button_horizontal_margin)
                v.updateLayoutParams<MarginLayoutParams> {
                    leftMargin = buttonHorizontalMargin.roundToInt() + insets.left
                }
                WindowInsetsCompat.CONSUMED
            }
        }

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
