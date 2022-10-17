package com.woocommerce.android.ui.simplifiedlogin.prologue

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.FeatureFlag
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class LoginPrologueFragment : BaseFragment(R.layout.fragment_login_prologue) {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentLoginPrologueBinding.bind(view)

        binding.buttonLoginStore.setOnClickListener {
            // Login with site address
            TODO()
        }

        binding.buttonLoginWpcom.setOnClickListener {
            // Login with WordPress.com account
            TODO()
        }

        binding.newToWooButton.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.LOGIN_NEW_TO_WOO_BUTTON_TAPPED)
            ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.NEW_TO_WOO_DOC)
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)
        }

        binding.buttonGetStarted.isVisible = FeatureFlag.STORE_CREATION_FLOW.isEnabled()
        binding.buttonGetStarted.setOnClickListener {
            findNavController().navigate(LoginPrologueFragmentDirections.actionLoginPrologueFragmentToSignupFragment())
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE)
    }
}
