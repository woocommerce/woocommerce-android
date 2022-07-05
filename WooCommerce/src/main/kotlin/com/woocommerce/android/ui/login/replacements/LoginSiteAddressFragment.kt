package com.woocommerce.android.ui.login.replacements

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.woocommerce.android.R
import dagger.android.support.AndroidSupportInjection
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.ERRONEOUS_SSL_CERTIFICATE
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.GENERIC_ERROR
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.HTTP_AUTH_REQUIRED
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.INVALID_URL
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.MISSING_XMLRPC_METHOD
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.NO_SITE_ERROR
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.WORDPRESS_COM_SITE
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.XMLRPC_BLOCKED
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError.XMLRPC_FORBIDDEN
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.BuildConfig
import org.wordpress.android.login.LoginBaseDiscoveryFragment
import org.wordpress.android.login.LoginBaseDiscoveryFragment.LoginBaseDiscoveryListener
import org.wordpress.android.login.LoginHttpAuthDialogFragment
import org.wordpress.android.login.LoginListener.SelfSignedSSLCallback
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment
import org.wordpress.android.login.LoginSiteAddressValidator
import org.wordpress.android.login.util.SiteUtils
import org.wordpress.android.login.widgets.WPLoginInputRow
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EditTextUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class LoginSiteAddressFragment : LoginBaseDiscoveryFragment(), TextWatcher, OnEditorCommitListener,
    LoginBaseDiscoveryListener {
    private var mSiteAddressInput: WPLoginInputRow? = null
    private var mRequestedSiteAddress: String? = null
    private var mConnectSiteInfoUrl: String? = null
    private var mConnectSiteInfoUrlRedirect: String? = null
    private var mConnectSiteInfoCalculatedHasJetpack = false
    private var mLoginSiteAddressValidator: LoginSiteAddressValidator? = null

    @Inject lateinit var mAccountStore: AccountStore

    @Inject lateinit var mDispatcher: Dispatcher

    @Inject lateinit var mHTTPAuthManager: HTTPAuthManager

    @Inject lateinit var mMemorizingTrustManager: MemorizingTrustManager

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.login_site_address_screen
    }

    @LayoutRes
    override fun getProgressBarText(): Int {
        return R.string.login_checking_site_address
    }

    override fun setupLabel(label: TextView) {
        if (mLoginListener.loginMode == LoginMode.SHARE_INTENT) {
            label.setText(R.string.enter_site_address_share_intent)
        } else {
            label.setText(R.string.enter_site_address)
        }
    }

    override fun setupContent(rootView: ViewGroup) {
        // important for accessibility - talkback
        requireActivity().setTitle(R.string.site_address_login_title)
        mSiteAddressInput = rootView.findViewById(R.id.login_site_address_row)
        mSiteAddressInput?.let {
            if (BuildConfig.DEBUG) {
                it.getEditText().setText(BuildConfig.DEBUG_WPCOM_WEBSITE_URL)
            }
            it.addTextChangedListener(this)
            it.setOnEditorCommitListener(this)
        }
        rootView.findViewById<View>(R.id.login_site_address_help_button).setOnClickListener {
            mAnalyticsListener.trackShowHelpClick()
            showSiteAddressHelp()
        }
    }

    override fun setupBottomButton(button: Button) {
        button.setOnClickListener { discover() }
    }

    override fun buildToolbar(toolbar: Toolbar, actionBar: ActionBar) {
        actionBar.setTitle(R.string.log_in)
    }

    override fun getEditTextToFocusOnStart(): EditText {
        return mSiteAddressInput!!.editText
    }

    override fun onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpSiteAddress(mRequestedSiteAddress)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            mRequestedSiteAddress =
                savedInstanceState.getString(KEY_REQUESTED_SITE_ADDRESS)
            mConnectSiteInfoUrl = savedInstanceState.getString(KEY_SITE_INFO_URL)
            mConnectSiteInfoUrlRedirect =
                savedInstanceState.getString(KEY_SITE_INFO_URL_AFTER_REDIRECTS)
            mConnectSiteInfoCalculatedHasJetpack =
                savedInstanceState.getBoolean(KEY_SITE_INFO_CALCULATED_HAS_JETPACK)
        } else {
            mAnalyticsListener.trackUrlFormViewed()
        }
        mLoginSiteAddressValidator = LoginSiteAddressValidator()
        mLoginSiteAddressValidator?.isValid?.observe(viewLifecycleOwner) { enabled ->
            bottomButton.isEnabled = enabled!!
        }
        mLoginSiteAddressValidator!!.errorMessageResId.observe(viewLifecycleOwner) { resId ->
            resId?.let {
                showError(it)
            } ?: mSiteAddressInput?.setError(null)
        }
    }

    override fun onResume() {
        super.onResume()
        mAnalyticsListener.siteAddressFormScreenResumed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_REQUESTED_SITE_ADDRESS, mRequestedSiteAddress)
        outState.putString(KEY_SITE_INFO_URL, mConnectSiteInfoUrl)
        outState.putString(
            KEY_SITE_INFO_URL_AFTER_REDIRECTS,
            mConnectSiteInfoUrlRedirect
        )
        outState.putBoolean(
            KEY_SITE_INFO_CALCULATED_HAS_JETPACK,
            mConnectSiteInfoCalculatedHasJetpack
        )
    }

    override fun onDestroyView() {
        mLoginSiteAddressValidator!!.dispose()
        mSiteAddressInput = null
        super.onDestroyView()
    }

    private fun discover() {
        if (!NetworkUtils.checkConnection(activity)) {
            return
        }
        mAnalyticsListener.trackSubmitClicked()
        mLoginBaseDiscoveryListener = this
        mRequestedSiteAddress = mLoginSiteAddressValidator!!.getCleanedSiteAddress()
        val cleanedXmlrpcSuffix = UrlUtils.removeXmlrpcSuffix(mRequestedSiteAddress)
        mAnalyticsListener.trackConnectedSiteInfoRequested(cleanedXmlrpcSuffix)
        mDispatcher.dispatch(SiteActionBuilder.newFetchConnectSiteInfoAction(cleanedXmlrpcSuffix))
        startProgress()
    }

    override fun onEditorCommit() {
        if (bottomButton.isEnabled) {
            discover()
        }
    }

    override fun afterTextChanged(s: Editable) {
        if (mSiteAddressInput != null) {
            mLoginSiteAddressValidator?.setAddress(EditTextUtils.getText(mSiteAddressInput!!.editText))
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        mConnectSiteInfoUrl = null
        mConnectSiteInfoUrlRedirect = null
        mConnectSiteInfoCalculatedHasJetpack = false
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (mSiteAddressInput != null) {
            mSiteAddressInput!!.setError(null)
        }
    }

    private fun showError(messageId: Int) {
        val message = getString(messageId)
        mAnalyticsListener.trackFailure(message)
        mSiteAddressInput!!.setError(message)
    }

    override fun endProgress() {
        super.endProgress()
        mRequestedSiteAddress = null
    }

    override fun getRequestedSiteAddress(): String {
        return mRequestedSiteAddress!!
    }

    override fun handleDiscoveryError(error: DiscoveryError, failedEndpoint: String) {
        when (error) {
            ERRONEOUS_SSL_CERTIFICATE -> mLoginListener.handleSslCertificateError(
                mMemorizingTrustManager,
                SelfSignedSSLCallback {
                    if (failedEndpoint == null) {
                        return@SelfSignedSSLCallback
                    }
                    // retry site lookup
                    discover()
                })
            HTTP_AUTH_REQUIRED -> askForHttpAuthCredentials(
                failedEndpoint,
                R.string.login_error_xml_rpc_cannot_read_site_auth_required
            )
            NO_SITE_ERROR -> showError(R.string.no_site_error)
            INVALID_URL -> {
                showError(R.string.invalid_site_url_message)
                mAnalyticsListener.trackInsertedInvalidUrl()
            }
            MISSING_XMLRPC_METHOD -> showError(R.string.xmlrpc_missing_method_error)
            WORDPRESS_COM_SITE -> {}
            XMLRPC_BLOCKED -> showError(R.string.xmlrpc_post_blocked_error)
            XMLRPC_FORBIDDEN -> showError(R.string.xmlrpc_endpoint_forbidden_error)
            GENERIC_ERROR -> showError(org.wordpress.android.login.R.string.error_generic)
        }
    }

    override fun handleWpComDiscoveryError(failedEndpoint: String) {
        AppLog.e(AppLog.T.API, "Inputted a wpcom address in site address screen.")

        // If the user is already logged in a wordpress.com account, bail out
        if (mAccountStore.hasAccessToken()) {
            val currentUsername = mAccountStore.account.userName
            AppLog.e(AppLog.T.NUX, "User is already logged in WordPress.com: $currentUsername")
            val oldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, true)
            mLoginListener.alreadyLoggedInWpcom(oldSitesIDs)
        } else {
            mLoginListener.gotWpcomSiteInfo(failedEndpoint)
        }
    }

    override fun handleDiscoverySuccess(endpointAddress: String) {
        AppLog.i(AppLog.T.NUX, "Discovery succeeded, endpoint: $endpointAddress")

        // hold the URL in a variable to use below otherwise it gets cleared up by endProgress
        val inputSiteAddress = mRequestedSiteAddress
        endProgress()
        if (mLoginListener.loginMode == LoginMode.WOO_LOGIN_MODE) {
            mLoginListener.gotConnectedSiteInfo(
                mConnectSiteInfoUrl!!,
                mConnectSiteInfoUrlRedirect,
                mConnectSiteInfoCalculatedHasJetpack
            )
        } else {
            mLoginListener.gotXmlRpcEndpoint(inputSiteAddress, endpointAddress)
        }
    }

    private fun askForHttpAuthCredentials(url: String, messageId: Int) {
        val loginHttpAuthDialogFragment = LoginHttpAuthDialogFragment.newInstance(
            url,
            getString(messageId)
        )
        loginHttpAuthDialogFragment.setTargetFragment(this, LoginHttpAuthDialogFragment.DO_HTTP_AUTH)
        loginHttpAuthDialogFragment.show(parentFragmentManager, LoginHttpAuthDialogFragment.TAG)
    }

    private fun showSiteAddressHelp() {
        LoginSiteAddressHelpDialogFragment().show(parentFragmentManager, LoginSiteAddressHelpDialogFragment.TAG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LoginHttpAuthDialogFragment.DO_HTTP_AUTH && resultCode == Activity.RESULT_OK) {
            val url = data!!.getStringExtra(LoginHttpAuthDialogFragment.ARG_URL)
            val httpUsername = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_USERNAME)
            val httpPassword = data.getStringExtra(LoginHttpAuthDialogFragment.ARG_PASSWORD)
            mHTTPAuthManager.addHTTPAuthCredentials(httpUsername!!, httpPassword!!, url!!, null)
            discover()
        }
    }

    // OnChanged events
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFetchedConnectSiteInfo(event: SiteStore.OnConnectSiteInfoChecked) {
        if (mRequestedSiteAddress == null) {
            // bail if user canceled
            return
        }
        if (!isAdded) {
            return
        }
        if (event.isError()) {
            mAnalyticsListener.trackConnectedSiteInfoFailed(
                mRequestedSiteAddress,
                event.javaClass.getSimpleName(),
                event.error.type.name,
                event.error.message
            )
            AppLog.e(AppLog.T.API, "onFetchedConnectSiteInfo has error: " + event.error.message)
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                showError(R.string.invalid_site_url_message)
            } else {
                showError(R.string.error_generic_network)
            }
            endProgressIfNeeded()
        } else {
            val hasJetpack = calculateHasJetpack(event.info)
            mConnectSiteInfoUrl = event.info.url
            mConnectSiteInfoUrlRedirect = event.info.urlAfterRedirects
            mConnectSiteInfoCalculatedHasJetpack = hasJetpack
            mAnalyticsListener.trackConnectedSiteInfoSucceeded(createConnectSiteInfoProperties(event.info, hasJetpack))
            if (mLoginListener.loginMode == LoginMode.WOO_LOGIN_MODE) {
                handleConnectSiteInfoForWoo(event.info)
            } else if (!event.info.isWPCom && mLoginListener.loginMode == LoginMode.JETPACK_LOGIN_ONLY) {
                handleConnectSiteInfoForJetpack(event.info)
            } else {
                handleConnectSiteInfoForWordPress(event.info)
            }
        }
    }

    private fun handleConnectSiteInfoForWoo(siteInfo: SiteStore.ConnectSiteInfoPayload) {
        if (!siteInfo.exists) {
            endProgressIfNeeded()
            // Site does not exist
            showError(R.string.invalid_site_url_message)
        } else if (!siteInfo.isWordPress) {
            endProgressIfNeeded()
            // Not a WordPress site
            mLoginListener.handleSiteAddressError(siteInfo)
        } else if (mConnectSiteInfoCalculatedHasJetpack) {
            endProgressIfNeeded()
            mLoginListener.gotConnectedSiteInfo(
                mConnectSiteInfoUrl!!,
                mConnectSiteInfoUrlRedirect,
                mConnectSiteInfoCalculatedHasJetpack
            )
        } else {
            /**
             * Jetpack internally uses xml-rpc protocol. Due to a bug on the API, when jetpack is
             * setup and connected to a .com account `isJetpackConnected` returns false when xml-rpc
             * is disabled.
             * This is causing issues to the client apps as they can't differentiate between
             * "xml-rpc disabled" and "jetpack not connected" states. Therefore, the login flow
             * library needs to invoke "xml-rpc discovery" to check if xml-rpc is accessible.
             */
            initiateDiscovery()
        }
    }

    private fun handleConnectSiteInfoForWordPress(siteInfo: SiteStore.ConnectSiteInfoPayload) {
        if (siteInfo.isWPCom) {
            // It's a Simple or Atomic site
            if (mLoginListener.loginMode == LoginMode.SELFHOSTED_ONLY) {
                // We're only interested in self-hosted sites
                if (siteInfo.hasJetpack) {
                    // This is an Atomic site, so treat it as self-hosted and start the discovery process
                    initiateDiscovery()
                    return
                }
            }
            endProgressIfNeeded()
            mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(siteInfo.url))
        } else {
            // It's a Jetpack or self-hosted site
            if (mLoginListener.loginMode == LoginMode.WPCOM_LOGIN_ONLY) {
                // We're only interested in WordPress.com accounts
                showError(R.string.enter_wpcom_or_jetpack_site)
                endProgressIfNeeded()
            } else {
                // Start the discovery process
                initiateDiscovery()
            }
        }
    }

    private fun handleConnectSiteInfoForJetpack(siteInfo: SiteStore.ConnectSiteInfoPayload) {
        endProgressIfNeeded()
        if (siteInfo.hasJetpack && siteInfo.isJetpackConnected && siteInfo.isJetpackActive) {
            mLoginListener.gotWpcomSiteInfo(UrlUtils.removeScheme(siteInfo.url))
        } else {
            mLoginListener.handleSiteAddressError(siteInfo)
        }
    }

    private fun calculateHasJetpack(siteInfo: SiteStore.ConnectSiteInfoPayload): Boolean {
        // Determining if jetpack is actually installed takes additional logic. This final
        // calculated event property will make querying this event more straight-forward.
        // Internal reference: p99K0U-1vO-p2#comment-3574
        var hasJetpack = false
        if (siteInfo.isWPCom && siteInfo.hasJetpack) {
            // This is likely an atomic site.
            hasJetpack = true
        } else if (siteInfo.isJetpackConnected) {
            hasJetpack = true
        }
        return hasJetpack
    }

    private fun createConnectSiteInfoProperties(
        siteInfo: SiteStore.ConnectSiteInfoPayload,
        hasJetpack: Boolean
    ): Map<String, String?> {
        val properties = HashMap<String, String?>()
        properties[KEY_SITE_INFO_URL] = siteInfo.url
        properties[KEY_SITE_INFO_URL_AFTER_REDIRECTS] = siteInfo.urlAfterRedirects
        properties[KEY_SITE_INFO_EXISTS] = siteInfo.exists.toString()
        properties[KEY_SITE_INFO_HAS_JETPACK] = siteInfo.hasJetpack.toString()
        properties[KEY_SITE_INFO_IS_JETPACK_ACTIVE] = siteInfo.isJetpackActive.toString()
        properties[KEY_SITE_INFO_IS_JETPACK_CONNECTED] = siteInfo.isJetpackConnected.toString()
        properties[KEY_SITE_INFO_IS_WORDPRESS] = siteInfo.isWordPress.toString()
        properties[KEY_SITE_INFO_IS_WPCOM] = siteInfo.isWPCom.toString()
        properties[KEY_SITE_INFO_CALCULATED_HAS_JETPACK] = hasJetpack.toString()
        return properties
    }

    companion object {
        private const val KEY_REQUESTED_SITE_ADDRESS = "KEY_REQUESTED_SITE_ADDRESS"
        private const val KEY_SITE_INFO_URL = "url"
        private const val KEY_SITE_INFO_URL_AFTER_REDIRECTS = "url_after_redirects"
        private const val KEY_SITE_INFO_EXISTS = "exists"
        private const val KEY_SITE_INFO_HAS_JETPACK = "has_jetpack"
        private const val KEY_SITE_INFO_IS_JETPACK_ACTIVE = "is_jetpack_active"
        private const val KEY_SITE_INFO_IS_JETPACK_CONNECTED = "is_jetpack_connected"
        private const val KEY_SITE_INFO_IS_WORDPRESS = "is_wordpress"
        private const val KEY_SITE_INFO_IS_WPCOM = "is_wp_com"
        private const val KEY_SITE_INFO_CALCULATED_HAS_JETPACK = "login_calculated_has_jetpack"
        const val TAG = "login_site_address_fragment_tag"
    }
}
