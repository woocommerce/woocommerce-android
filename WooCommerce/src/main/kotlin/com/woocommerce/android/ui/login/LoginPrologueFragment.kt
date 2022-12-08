package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.experiment.SimplifiedLoginExperiment
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant.SIMPLIFIED_LOGIN_WPCOM
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
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
        fun onGetStartedClicked()
    }

    constructor() : this(R.layout.fragment_login_prologue)

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker
    @Inject lateinit var simplifiedLoginExperiment: SimplifiedLoginExperiment
    private var prologueFinishedListener: PrologueFinishedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val isSimplifiedLoginVariant = simplifiedLoginExperiment.getCurrentVariant() == SIMPLIFIED_LOGIN_WPCOM

        val binding = FragmentLoginPrologueBinding.bind(view)

        binding.buttonLoginStore.setOnClickListener {
            // Login with site address
            AppPrefs.setStoreCreationSource(AnalyticsTracker.VALUE_LOGIN)
            prologueFinishedListener?.onPrimaryButtonClicked()
        }

        binding.buttonLoginWpcom.setOnClickListener {
            // Login with WordPress.com account
            AppPrefs.setStoreCreationSource(AnalyticsTracker.VALUE_LOGIN)
            prologueFinishedListener?.onSecondaryButtonClicked()
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)

            simplifiedLoginExperiment.activate()
        }

        binding.buttonGetStarted.setOnClickListener {
            AppPrefs.setStoreCreationSource(AnalyticsTracker.VALUE_PROLOGUE)
            AnalyticsTracker.track(stat = AnalyticsEvent.LOGIN_PROLOGUE_CREATE_SITE_TAPPED)
            prologueFinishedListener?.onGetStartedClicked()
        }

        if (isSimplifiedLoginVariant) setupSimplifiedLoginVariant(binding)
    }

    private fun setupSimplifiedLoginVariant(binding: FragmentLoginPrologueBinding) {
        binding.orDivider.root.hide()
        binding.buttonLoginWpcom.text = getString(R.string.log_in)

        binding.buttonLoginStore.hide()
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
