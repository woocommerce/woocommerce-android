package com.woocommerce.android.ui.login

import android.content.Context
import android.os.Bundle
import android.view.View
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

    interface PrologueFinishedListener {
        fun onPrimaryButtonClicked()
        fun onSecondaryButtonClicked()
        fun onNewToWooButtonClicked()
    }

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    private var prologueFinishedListener: PrologueFinishedListener? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with(FragmentLoginPrologueBinding.bind(view)) {
            buttonLoginStore.setOnClickListener {
                // Login with site address
                prologueFinishedListener?.onPrimaryButtonClicked()
            }

            buttonLoginWpcom.setOnClickListener {
                // Login with WordPress.com account
                prologueFinishedListener?.onSecondaryButtonClicked()
            }

            buttonStartNewStore.setOnClickListener {
                AnalyticsTracker.track(
                    AnalyticsEvent.LOGIN_PROLOGUE_STARTING_A_NEW_STORE_TAPPED
                )

                prologueFinishedListener?.onNewToWooButtonClicked()
            }
        }

        if (savedInstanceState == null) {
            unifiedLoginTracker.track(Flow.PROLOGUE, Step.PROLOGUE)
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
