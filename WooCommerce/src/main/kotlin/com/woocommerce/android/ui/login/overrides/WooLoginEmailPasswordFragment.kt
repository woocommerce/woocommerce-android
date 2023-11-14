package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.bumptech.glide.Registry.MissingComponentException
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.widgets.WPLoginInputRow

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        @Suppress("LongParameterList")
        fun newInstance(
            emailAddress: String?,
            password: String? = null,
            idToken: String? = null,
            service: String? = null,
            isSocialLogin: Boolean = false,
            verifyMagicLinkEmail: Boolean = false
        ): WooLoginEmailPasswordFragment {
            val fragment = WooLoginEmailPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_SOCIAL_ID_TOKEN, idToken)
            args.putString(ARG_SOCIAL_SERVICE, service)
            args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin)
            args.putBoolean(ARG_ALLOW_MAGIC_LINK, false) // This hides the old link button
            args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyMagicLinkEmail)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null
    private var email: String? = null
    private var isSocialLogin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        email = requireArguments().getString(ARG_EMAIL_ADDRESS)
        isSocialLogin = requireArguments().getBoolean(ARG_SOCIAL_LOGIN)
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        loginListener = if (context is LoginListener) {
            context
        } else {
            throw MissingComponentException("$context must implement LoginListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        loginListener = null
    }

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_password

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)

        rootView.findViewById<Button>(R.id.bottom_button_magic_link)?.apply {
            isVisible = true // this button was intentionally hidden until the password screen is shown
            setOnClickListener {
                loginListener?.useMagicLinkInstead(email, false)
            }
        }

        val prefilledPassword = requireArguments().getString(ARG_PASSWORD)
        if (prefilledPassword.isNotNullOrEmpty()) {
            rootView.findViewById<WPLoginInputRow>(R.id.login_password_row)
        }
    }
}
