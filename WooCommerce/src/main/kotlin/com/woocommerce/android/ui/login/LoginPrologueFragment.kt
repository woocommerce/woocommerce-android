package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.experiment.SimplifiedLoginExperiment
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant.SIMPLIFIED
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.util.FeatureFlag
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class LoginPrologueFragment(@LayoutRes layout: Int) : Fragment(layout) {
    companion object {
        const val TAG = "login-prologue-fragment"
    }

    interface PrologueFinishedListener {
        fun onPrimaryButtonClicked()
        fun onSecondaryButtonClicked()
        fun onNewToWooButtonClicked()
    }

    constructor() : this(R.layout.fragment_login_prologue)

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Inject lateinit var simplifiedLoginExperiment: SimplifiedLoginExperiment
    private var prologueFinishedListener: PrologueFinishedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSimplifiedLoginVariant = simplifiedLoginExperiment.getCurrentVariant() == SIMPLIFIED

        val binding = FragmentLoginPrologueBinding.bind(view)

        binding.buttonLoginStore.setOnClickListener {
            // Login with site address
            prologueFinishedListener?.onPrimaryButtonClicked()
        }

        binding.buttonLoginWpcom.setOnClickListener {
            // Login with WordPress.com account
            prologueFinishedListener?.onSecondaryButtonClicked()
        }

        binding.newToWooButton.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_NEW_TO_WOO_BUTTON_TAPPED)
            prologueFinishedListener?.onNewToWooButtonClicked()
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)
        }

        binding.buttonGetStarted.isVisible = FeatureFlag.ACCOUNT_CREATION_FLOW.isEnabled()
        binding.buttonGetStarted.setOnClickListener {
            findNavController().navigate(LoginPrologueFragmentDirections.actionLoginPrologueFragmentToSignupFragment())
        }

        // Various updates related to Simplified Login A/B testing.
        if (isSimplifiedLoginVariant) {
            binding.buttonLoginStore.hide()
            binding.newToWooButton.text = getString(R.string.login_prologue_learn_more_about_woo)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is PrologueFinishedListener) {
            prologueFinishedListener = activity as PrologueFinishedListener
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE)
    }

    override fun onDetach() {
        super.onDetach()
        prologueFinishedListener = null
    }
}
