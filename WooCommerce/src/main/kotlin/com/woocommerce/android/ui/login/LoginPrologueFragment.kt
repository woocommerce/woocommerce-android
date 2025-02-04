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
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueBinding
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginPrologueFragment : Fragment(R.layout.fragment_login_prologue) {
    companion object {
        const val TAG = "login-prologue-fragment"
    }

    interface PrologueListener {
        fun onPrimaryButtonClicked()
        fun onSecondaryButtonClicked()
        fun onNewToWooButtonClicked()
    }

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private var prologueListener: PrologueListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? DynamicEdgeToEdgeActivity)?.enableDynamicEdgeToEdge(forceDarkStatusBar = true)

        with(FragmentLoginPrologueBinding.bind(view)) {
            ViewCompat.setOnApplyWindowInsetsListener(loginButtons) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.updateLayoutParams<MarginLayoutParams> { bottomMargin = insets.bottom }
                WindowInsetsCompat.CONSUMED
            }

            buttonLoginStore.setOnClickListener {
                // Login with site address
                prologueListener?.onPrimaryButtonClicked()
            }

            buttonLoginWpcom.setOnClickListener {
                // Login with WordPress.com account
                prologueListener?.onSecondaryButtonClicked()
            }

            buttonStartNewStore.setOnClickListener {
                AnalyticsTracker.track(
                    AnalyticsEvent.LOGIN_PROLOGUE_STARTING_A_NEW_STORE_TAPPED
                )

                prologueListener?.onNewToWooButtonClicked()
            }
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is PrologueListener) {
            prologueListener = activity as PrologueListener
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, Step.PROLOGUE)
    }

    override fun onDetach() {
        super.onDetach()
        prologueListener = null
    }
}
