package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueSurveyBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginPrologueSurveyFragment : Fragment(R.layout.fragment_login_prologue_survey) {
    companion object {
        const val TAG = "login-prologue-survey-fragment"

        fun newInstance(): LoginPrologueSurveyFragment {
            return LoginPrologueSurveyFragment()
        }
    }

    interface PrologueSurveyListener {
        fun onCarouselFinished()
    }

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    private var prologueSurveyListener: PrologueSurveyListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLoginPrologueSurveyBinding.bind(view)

        binding.surveyButtonNext.setOnClickListener {
        }

        binding.surveyButtonSkip.setOnClickListener {
            prologueSurveyListener?.onCarouselFinished()
        }

        binding.surveyButtonExploring.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButtonFreePlugin.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButtonSetUpStore.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButtonMultipleStores.setOnClickListener {
            updateSelection(binding, it)
        }

        binding.surveyButtonFeaturesAvailable.setOnClickListener {
            updateSelection(binding, it)
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE_SURVEY)
        }
    }

    private fun updateSelection(binding: FragmentLoginPrologueSurveyBinding, tappedButton: View) {
        binding.surveyButtonExploring.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButtonFreePlugin.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButtonSetUpStore.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButtonMultipleStores.takeIf { it != tappedButton }?.isSelected = false
        binding.surveyButtonFeaturesAvailable.takeIf { it != tappedButton }?.isSelected = false
        tappedButton.isSelected = !tappedButton.isSelected
        binding.surveyButtonNext.isEnabled = tappedButton.isSelected
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
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
