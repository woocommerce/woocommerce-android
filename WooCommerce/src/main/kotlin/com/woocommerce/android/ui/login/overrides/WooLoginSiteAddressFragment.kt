package com.woocommerce.android.ui.login.overrides

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import org.wordpress.android.login.LoginSiteAddressFragment
import org.wordpress.android.login.widgets.WPLoginInputRow

class WooLoginSiteAddressFragment : LoginSiteAddressFragment() {
    companion object {
        private const val ARG_PREFILLED_SITE_URL = "prefilled_site_url"

        fun newInstance(prefilledSiteUrl: String? = null): WooLoginSiteAddressFragment =
            WooLoginSiteAddressFragment().apply {
                arguments = Bundle().apply { putString(ARG_PREFILLED_SITE_URL, prefilledSiteUrl) }
            }
    }

    @LayoutRes
    override fun getContentLayout() = R.layout.fragment_login_site_address

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefilledSiteUrl = arguments?.getString(ARG_PREFILLED_SITE_URL)
        if (prefilledSiteUrl.isNotNullOrEmpty()) {
            view.findViewById<WPLoginInputRow>(R.id.login_site_address_row)
                ?.editText
                ?.setText(prefilledSiteUrl)
            // Auto-submit so the merchant goes from "scan QR" to the next login step with no taps,
            // mirroring WooLoginEmailFragment which calls next(prefilledEmail) on the email screen.
            discover()
            // Single-shot: prevent replay on rotation / process recovery.
            arguments?.remove(ARG_PREFILLED_SITE_URL)
        }
    }
}
