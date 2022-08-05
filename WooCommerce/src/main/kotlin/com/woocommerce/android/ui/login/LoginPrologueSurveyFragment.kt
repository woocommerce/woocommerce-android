package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_ONBOARDING_SURVEY_SHOWN
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_ONBOARDING_SURVEY_SUBMITTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_OPTION
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.databinding.FragmentLoginPrologueSurveyBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginPrologueSurveyFragment : Fragment(R.layout.fragment_login_prologue_survey) {
    companion object {
        const val TAG = "login-prologue-survey-fragment"

        const val EXPLORING_OPTION = "exploring_stores"
        const val SETTING_UP_STORE_OPTION = "setting_up_stores_stores"
        const val ANALYTICS_OPTION = "analytics_stores"
        const val PRODUCTS_OPTION = "products_stores"
        const val ORDERS_OPTION = "orders_stores"
        const val MULTIPLE_STORES_OPTION = "multiple_stores"

        fun newInstance(): LoginPrologueSurveyFragment {
            return LoginPrologueSurveyFragment()
        }
    }

    interface PrologueSurveyListener {
        fun onSurveyFinished()
    }

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Inject lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private var prologueSurveyListener: PrologueSurveyListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLoginPrologueSurveyBinding.bind(view)

        binding.surveyButtonNext.setOnClickListener {
            val option = when {
                binding.surveyButton1.isSelected -> EXPLORING_OPTION
                binding.surveyButton2.isSelected -> SETTING_UP_STORE_OPTION
                binding.surveyButton3.isSelected -> ANALYTICS_OPTION
                binding.surveyButton4.isSelected -> PRODUCTS_OPTION
                binding.surveyButton5.isSelected -> ORDERS_OPTION
                binding.surveyButton6.isSelected -> MULTIPLE_STORES_OPTION
                else -> ""
            }

            analyticsTrackerWrapper.track(
                LOGIN_ONBOARDING_SURVEY_SUBMITTED,
                mapOf(Pair(KEY_OPTION, option))
            )

            prologueSurveyListener?.onSurveyFinished()
        }

        binding.surveyButtonSkip.setOnClickListener {
            prologueSurveyListener?.onSurveyFinished()
        }

        binding.surveyButton1.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButton2.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButton3.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButton4.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButton5.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButton6.setOnClickListener {
            updateSelection(binding, it)
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE_SURVEY)
        }

        analyticsTrackerWrapper.track(LOGIN_ONBOARDING_SURVEY_SHOWN)
    }

    private fun updateSelection(binding: FragmentLoginPrologueSurveyBinding, tappedButton: View) {
        binding.surveyButton1.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButton2.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButton3.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButton4.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButton5.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButton6.takeIf { it != tappedButton }?.isSelected = false
        tappedButton.isSelected = !tappedButton.isSelected
        binding.surveyButtonNext.isEnabled = tappedButton.isSelected
    }

    override fun onResume() {
        super.onResume()
        analyticsTrackerWrapper.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE_SURVEY)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is PrologueSurveyListener) {
            prologueSurveyListener = activity as PrologueSurveyListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        prologueSurveyListener = null
    }
}
