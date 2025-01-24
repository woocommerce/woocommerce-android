package com.woocommerce.android.ui.login.overrides

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.woocommerce.android.R
import org.wordpress.android.login.LoginEmailPasswordFragment

class WooLoginEmailPasswordFragment : LoginEmailPasswordFragment() {
    companion object {
        @Suppress("LongParameterList")
        fun newInstance(
            emailAddress: String?,
            idToken: String? = null,
            service: String? = null,
            isSocialLogin: Boolean = false,
            verifyMagicLinkEmail: Boolean = false
        ): WooLoginEmailPasswordFragment {
            val fragment = WooLoginEmailPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, emailAddress)
            args.putString(ARG_SOCIAL_ID_TOKEN, idToken)
            args.putString(ARG_SOCIAL_SERVICE, service)
            args.putBoolean(ARG_SOCIAL_LOGIN, isSocialLogin)
            args.putBoolean(ARG_ALLOW_MAGIC_LINK, false) // This hides the old link button
            args.putBoolean(ARG_VERIFY_MAGIC_LINK_EMAIL, verifyMagicLinkEmail)
            fragment.arguments = args
            return fragment
        }
    }

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_password

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)

        // Replace the original magic link button with the new one in bottom section
        val originalMagicLinkButton = rootView.findViewById<Button>(R.id.login_get_email_link)
        rootView.findViewById<Button>(R.id.bottom_button_magic_link)?.apply {
            isVisible = true // this button was intentionally hidden until the password screen is shown
            setOnClickListener {
                originalMagicLinkButton.performClick()
            }
        }
    }
}
