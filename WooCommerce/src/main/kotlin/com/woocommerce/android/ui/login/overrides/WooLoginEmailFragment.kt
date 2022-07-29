package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import org.wordpress.android.login.LoginEmailFragment

class WooLoginEmailFragment : LoginEmailFragment() {
    interface Listener {
        fun onWhatIsWordPressLinkClicked()
    }

    companion object {
        fun newInstance(siteAddress: String, optionalSiteCredsLayout: Boolean): WooLoginEmailFragment {
            val fragment = WooLoginEmailFragment()
            val args = Bundle()
            args.putString(ARG_LOGIN_SITE_URL, siteAddress)
            args.putBoolean(ARG_OPTIONAL_SITE_CREDS_LAYOUT, optionalSiteCredsLayout)
            fragment.arguments = args
            return fragment
        }

        const val ARG_LOGIN_SITE_URL = "ARG_LOGIN_SITE_URL"
        const val ARG_OPTIONAL_SITE_CREDS_LAYOUT = "ARG_OPTIONAL_SITE_CREDS_LAYOUT"
    }

    private lateinit var whatIsWordPressLinkClickListener: Listener
    private var optionalSiteCredsLayout = false
    private var loginSiteUrl = ""

    @LayoutRes
    override fun getContentLayout(): Int {
        return if (optionalSiteCredsLayout) {
            R.layout.fragment_login_email_optional_site_creds_screen
        } else {
            R.layout.fragment_login_email_screen
        }
    }

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        rootView.findViewById<Button>(R.id.login_what_is_wordpress).setOnClickListener {
            whatIsWordPressLinkClickListener.onWhatIsWordPressLinkClicked()
        }

        // Follow LoginEmailFragment's behavior to only show this button if not on "Site Credentials" layout.
        if (optionalSiteCredsLayout) {
            rootView.findViewById<Button>(R.id.continue_tos).hide()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            whatIsWordPressLinkClickListener = activity as Listener
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            optionalSiteCredsLayout = it.getBoolean(ARG_OPTIONAL_SITE_CREDS_LAYOUT, false)
            loginSiteUrl = it.getString(ARG_LOGIN_SITE_URL, "")
        }
    }
}
