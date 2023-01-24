package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.experiment.RESTAPILoginExperiment
import com.woocommerce.android.experiment.RESTAPILoginExperiment.RESTAPILoginVariant
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var restAPILoginExperiment: RESTAPILoginExperiment

    private var prologueFinishedListener: PrologueFinishedListener? = null

    private val loadingIndicator by lazy {
        CircularProgressIndicator(requireActivity()).apply {
            isIndeterminate = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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

        binding.buttonGetStarted.setOnClickListener {
            AppPrefs.setStoreCreationSource(AnalyticsTracker.VALUE_PROLOGUE)
            AnalyticsTracker.track(stat = AnalyticsEvent.LOGIN_PROLOGUE_CREATE_SITE_TAPPED)
            prologueFinishedListener?.onGetStartedClicked()
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)

            binding.showLoadingIndicator()

            lifecycleScope.launch {
                restAPILoginExperiment.activate()
                binding.removeLoadingIndicator()
                binding.handleRestLoginExperiment()
            }
        } else {
            binding.handleRestLoginExperiment()
        }
    }

    private fun FragmentLoginPrologueBinding.handleRestLoginExperiment() {
        // Override behavior for REST API treatment experiment
        if (restAPILoginExperiment.getCurrentVariant() == RESTAPILoginVariant.TREATMENT) {
            // Since Android doesn't allow changing view's styles, we will just swap buttons in order to have the store
            // button primary
            buttonLoginStore.hide()
            buttonLoginWpcom.text = buttonLoginStore.text
            buttonLoginWpcom.layoutParams = buttonLoginStore.layoutParams
            buttonLoginWpcom.setOnClickListener {
                // Forward click to site address button
                buttonLoginStore.performClick()
            }
        }
    }

    private fun FragmentLoginPrologueBinding.showLoadingIndicator() {
        loadingIndicator.layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = loginButtons.id
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }

        root.addView(loadingIndicator)
        loginButtons.visibility = View.INVISIBLE
    }

    private fun FragmentLoginPrologueBinding.removeLoadingIndicator() {
        root.removeView(loadingIndicator)
        loginButtons.show()
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
