package com.woocommerce.android.ui.login.overrides

import android.os.Bundle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.login.LoginUsernamePasswordFragment
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

/**
 * This class is a temporary implementation to be used during the development of the REST API project.
 * The implementation will be updated according to product specs at the end of the project.
 */
class WooLoginUsernamePasswordFragment : LoginUsernamePasswordFragment() {
    companion object {
        private const val ARG_INPUT_SITE_ADDRESS = "ARG_INPUT_SITE_ADDRESS"
        private const val ARG_ENDPOINT_ADDRESS = "ARG_ENDPOINT_ADDRESS"
        private const val ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME"
        private const val ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD"
        private const val ARG_IS_WPCOM = "ARG_IS_WPCOM"
        const val TAG = "login_username_password_fragment_tag"

        fun newInstance(
            inputSiteAddress: String?,
            endpointAddress: String?,
            inputUsername: String?,
            inputPassword: String?,
            isWpcom: Boolean
        ): WooLoginUsernamePasswordFragment {
            val fragment = WooLoginUsernamePasswordFragment()
            val args = Bundle()
            args.putString(ARG_INPUT_SITE_ADDRESS, inputSiteAddress)
            args.putString(ARG_ENDPOINT_ADDRESS, endpointAddress)
            args.putString(ARG_INPUT_USERNAME, inputUsername)
            args.putString(ARG_INPUT_PASSWORD, inputPassword)
            args.putBoolean(ARG_IS_WPCOM, isWpcom)
            fragment.arguments = args
            return fragment
        }
    }

    @Inject
    lateinit var selectedSite: SelectedSite

    override fun onSiteChanged(event: OnSiteChanged) {
        if (!event.isError && FeatureFlag.REST_API.isEnabled()) {
            val siteAddress = requireArguments().getString(ARG_INPUT_SITE_ADDRESS)
            val site = SiteUtils.getXMLRPCSiteByUrl(mSiteStore, siteAddress)!!
            selectedSite.set(site)
            mLoginListener.loggedInViaUsernamePassword(SiteUtils.getCurrentSiteIds(mSiteStore, false))
        } else {
            super.onSiteChanged(event)
        }
    }
}
