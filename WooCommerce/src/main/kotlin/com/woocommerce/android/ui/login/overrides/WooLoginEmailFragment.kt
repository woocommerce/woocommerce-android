package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.showKeyboardWithDelay
import org.wordpress.android.login.LoginEmailFragment

class WooLoginEmailFragment : LoginEmailFragment() {
    companion object {
        private const val ARG_PREFILLED_EMAIL = "prefilled_email"
        private const val ARG_SHOW_SITE_CREDENTIALS_FALLBACK = "show_site_credentials_fallback"
        fun newInstance(
            prefilledEmail: String? = null,
            showSiteCredentialsFallback: Boolean = true
        ): LoginEmailFragment {
            val fragment = WooLoginEmailFragment()
            val args = Bundle()
            args.putString(ARG_PREFILLED_EMAIL, prefilledEmail)
            args.putBoolean(ARG_SHOW_SITE_CREDENTIALS_FALLBACK, showSiteCredentialsFallback)
            fragment.arguments = args
            return fragment
        }
    }

    interface Listener {
        fun onWhatIsWordPressLinkClicked()
        fun onLoginWithSiteCredentialsFallbackClicked()
    }

    private lateinit var wooLoginEmailListener: Listener

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_screen

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        val whatIsWordPressText = rootView.findViewById<Button>(R.id.login_what_is_wordpress)
        whatIsWordPressText.setOnClickListener {
            wooLoginEmailListener.onWhatIsWordPressLinkClicked()
        }
        val showSiteCredentialsFallback = requireArguments().getBoolean(ARG_SHOW_SITE_CREDENTIALS_FALLBACK, false)
        val loginWithSiteCredentials = rootView.findViewById<Button>(R.id.login_with_site_credentials)
        if (showSiteCredentialsFallback) {
            loginWithSiteCredentials.setOnClickListener {
                wooLoginEmailListener.onLoginWithSiteCredentialsFallbackClicked()
            }
            loginWithSiteCredentials.visibility = View.VISIBLE
        } else {
            loginWithSiteCredentials.visibility = View.GONE
        }
    }

    override fun setupLabel(label: TextView) {
        // NO-OP, For this custom screen, the correct label is set in the layout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefilledEmail = requireArguments().getString(ARG_PREFILLED_EMAIL)
        if (prefilledEmail.isNotNullOrEmpty()) {
            mEmailInput?.editText?.setText(prefilledEmail)
            next(prefilledEmail)
            requireArguments().clear()
        }
    }

    override fun onResume() {
        super.onResume()
        mEmailInput?.editText?.showKeyboardWithDelay(0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            wooLoginEmailListener = activity as Listener
        }
    }
}
