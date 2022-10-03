package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.bumptech.glide.Registry.MissingComponentException
import com.woocommerce.android.R
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.AUTOMATIC
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.CONTROL
import com.woocommerce.android.experiment.MagicLinkRequestExperiment.MagicLinkRequestVariant.ENHANCED
import com.woocommerce.android.extensions.requestPasswordAutoFillWithDelay
import com.woocommerce.android.extensions.showKeyboardWithDelay
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginWpcomService
import org.wordpress.android.login.widgets.WPLoginInputRow

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        private const val ARG_VARIANT = "ARG_VARIANT"

        @Suppress("LongParameterList")
        fun newInstance(
            emailAddress: String?,
            password: String? = null,
            idToken: String? = null,
            service: String? = null,
            isSocialLogin: Boolean = false,
            allowMagicLink: Boolean = false,
            verifyMagicLinkEmail: Boolean = false,
            variant: MagicLinkRequestVariant = CONTROL
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
            args.putString(ARG_VARIANT, variant.name)
            fragment.arguments = args
            return fragment
        }
    }

    interface Listener {
        fun onPasswordError()
    }

    private lateinit var onPassWordErrorListener: Listener
    private var loginListener: LoginListener? = null
    private var variant: MagicLinkRequestVariant = CONTROL
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.getString(ARG_VARIANT)?.let {
            variant = MagicLinkRequestVariant.valueOf(it)
        }

        email = arguments?.getString(ARG_EMAIL_ADDRESS)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        loginListener = if (context is LoginListener) {
            context
        } else {
            throw MissingComponentException("$context must implement LoginListener")
        }

        if (activity is Listener) {
            onPassWordErrorListener = activity as Listener
        }
    }

    override fun onDetach() {
        super.onDetach()
        loginListener = null
    }

    @LayoutRes
    override fun getContentLayout(): Int =
        when (variant) {
            CONTROL -> super.getContentLayout()
            ENHANCED,
            AUTOMATIC -> R.layout.fragment_login_email_password
        }

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        when (variant) {
            ENHANCED -> addRequestMagicLinkButton(rootView)
            AUTOMATIC -> addOpenEmailClientButton(rootView)
            CONTROL -> {
                with(rootView.findViewById<WPLoginInputRow>(R.id.login_password_row).editText) {
                    requestPasswordAutoFillWithDelay()
                    showKeyboardWithDelay()
                }
            }
        }
    }

    private fun addRequestMagicLinkButton(rootView: ViewGroup?) {
        rootView?.findViewById<View>(R.id.magic_link_message)?.isVisible = false
        rootView?.findViewById<Button>(R.id.bottom_button_magic_link)?.apply {
            isVisible = true
            setOnClickListener {
                loginListener?.useMagicLinkInstead(email, false)
            }
        }
    }

    private fun addOpenEmailClientButton(rootView: ViewGroup?) {
        rootView?.findViewById<Button>(R.id.button_login_open_email_client)?.setOnClickListener {
            loginListener?.openEmailClient(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    override fun onLoginStateUpdated(loginState: LoginWpcomService.LoginState) {
        super.onLoginStateUpdated(loginState)
        if (loginState.step == LoginWpcomService.LoginStep.FAILURE_EMAIL_WRONG_PASSWORD) {
            onPassWordErrorListener.onPasswordError()
        }
    }
}
