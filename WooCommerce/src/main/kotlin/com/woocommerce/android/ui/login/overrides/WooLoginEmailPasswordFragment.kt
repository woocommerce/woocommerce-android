package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginWpcomService.LoginState
import org.wordpress.android.login.LoginWpcomService.LoginStep.FAILURE_EMAIL_WRONG_PASSWORD

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        @Suppress("LongParameterList")
        fun newInstance(
            emailAddress: String?,
            password: String?,
            idToken: String?,
            service: String?,
            isSocialLogin: Boolean,
            allowMagicLink: Boolean = false,
            verifyMagicLinkEmail: Boolean = false
        ): WooLoginEmailPasswordFragment {
            val fragment = WooLoginEmailPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_SOCIAL_ID_TOKEN, idToken)
            args.putString(ARG_SOCIAL_SERVICE, service)
            args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin)
            args.putBoolean(ARG_ALLOW_MAGIC_LINK, allowMagicLink)
            args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyMagicLinkEmail)
            fragment.arguments = args
            return fragment
        }
    }

    interface Listener {
        fun onPasswordError()
    }

    private lateinit var onPassWordErrorListener: Listener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            onPassWordErrorListener = activity as Listener
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    override fun onLoginStateUpdated(loginState: LoginState) {
        super.onLoginStateUpdated(loginState)
        if (loginState.step == FAILURE_EMAIL_WRONG_PASSWORD) {
            onPassWordErrorListener.onPasswordError()
        }
    }
}
