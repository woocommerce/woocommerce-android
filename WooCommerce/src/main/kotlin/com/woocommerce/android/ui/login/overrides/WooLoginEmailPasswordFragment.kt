package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginListener

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        const val TAG = "woo_login_email_password_fragment_tag"

        private const val ARG_EMAIL_ADDRESS = "ARG_EMAIL_ADDRESS"
        private const val ARG_PASSWORD = "ARG_PASSWORD"
        private const val ARG_SOCIAL_ID_TOKEN = "ARG_SOCIAL_ID_TOKEN"
        private const val ARG_SOCIAL_LOGIN = "ARG_SOCIAL_LOGIN"
        private const val ARG_SOCIAL_SERVICE = "ARG_SOCIAL_SERVICE"
        private const val ARG_ALLOW_MAGIC_LINK = "ARG_ALLOW_MAGIC_LINK"
        private const val ARG_VERIFY_MAGIC_LINK_EMAIL = "ARG_VERIFY_MAGIC_LINK_EMAIL"

        fun newInstance(
            emailAddress: String?,
            password: String?,
            idToken: String?,
            service: String?,
            isSocialLogin: Boolean,
            verifyEmail: Boolean
        ): WooLoginEmailPasswordFragment {
            val fragment = WooLoginEmailPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putString(ARG_PASSWORD, password)
            args.putString(ARG_SOCIAL_ID_TOKEN, idToken)
            args.putString(ARG_SOCIAL_SERVICE, service)
            args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin)
            args.putBoolean(ARG_ALLOW_MAGIC_LINK, false)
            args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyEmail)
            fragment.arguments = args
            return fragment
        }
    }

    private var loginListener: LoginListener? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        loginListener = if (context is LoginListener) {
            context
        } else {
            throw RuntimeException("$context must implement LoginListener")
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

        rootView?.findViewById<Button>(R.id.button_login_open_email_client)?.setOnClickListener {
            loginListener?.openEmailClient(true)
        }
    }
}
