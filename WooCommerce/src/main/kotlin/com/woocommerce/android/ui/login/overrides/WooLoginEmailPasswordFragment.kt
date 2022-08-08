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
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginWpcomService

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        const val TAG = "woo_login_email_password_fragment_tag"

        private const val ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS"
        private const val ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN"
        private const val ARG_ALLOW_MAGIC_LINK = "ARG_ALLOW_MAGIC_LINK"
        private const val ARG_VERIFY_MAGIC_LINK_EMAIL = "ARG_VERIFY_MAGIC_LINK_EMAIL"
        private const val ARG_VARIANT = "ARG_VARIANT"

        fun newInstance(
            emailAddress: String?,
            verifyEmail: Boolean,
            variant: MagicLinkRequestVariant
        ): WooLoginEmailPasswordFragment {
            val fragment = WooLoginEmailPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putBoolean(ARG_SOCIAL_LOGIN, false)
            args.putBoolean(ARG_ALLOW_MAGIC_LINK, false)
            args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyEmail)
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
    override fun getContentLayout(): Int {
        return R.layout.fragment_login_email_password
    }

    override fun setupContent(rootView: ViewGroup?) {
        super.setupContent(rootView)

        if (variant == AUTOMATIC) {
            rootView?.findViewById<Button>(R.id.button_login_open_email_client)?.setOnClickListener {
                loginListener?.openEmailClient(true)
            }
        } else {
            rootView?.findViewById<View>(R.id.magic_link_message)?.isVisible = false
            rootView?.findViewById<Button>(R.id.bottom_button_magic_link)?.apply {
                isVisible = true
                setOnClickListener {
                    loginListener?.useMagicLinkInstead(email, false)
                }
            }
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
