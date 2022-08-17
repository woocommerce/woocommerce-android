package com.woocommerce.android.ui.sitediscovery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.ActivityLoginBinding
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.ZendeskExtraTags
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragmentArgs
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginJetpackRequiredFragment
import com.woocommerce.android.ui.login.LoginNoJetpackListener
import com.woocommerce.android.ui.login.LoginSiteCheckErrorFragment
import com.woocommerce.android.ui.login.LoginWhatIsJetpackDialogFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.login.AuthOptions
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginListener.SelfSignedSSLCallback
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressFragment
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * The goal of this Activity is to reuse the site discovery from the Login library, but since the login is tied
 * to the [LoginListener], we need to implement all functions even though we need just a small subset of them.
 */
@AndroidEntryPoint
class PostLoginSiteDiscoveryActivity : AppCompatActivity(), LoginListener, LoginNoJetpackListener {
    companion object {
        private const val JETPACK_CONNECT_URL = "https://wordpress.com/jetpack/connect"
        private const val JETPACK_CONNECTED_REDIRECT_URL = "woocommerce://jetpack-connected-post-login"
        private const val JETPACK_CONNECTED_REDIRECT_URL_QUERY = "url"
    }

    private val binding by lazy {
        ActivityLoginBinding.inflate(layoutInflater)
    }

    @Inject lateinit var crashLogging: CrashLogging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (hasJetpackConnectedIntent()) {
            val siteAddress = intent.data!!.getQueryParameter(JETPACK_CONNECTED_REDIRECT_URL_QUERY)!!
            // Save the site address to be able to continue login from the Site Picker
            AppPrefs.setLoginSiteAddress(siteAddress)
            showMainActivityAndFinish()
        } else {
            loginViaSiteAddress()
        }
    }

    private fun hasJetpackConnectedIntent(): Boolean {
        val action = intent.action
        val uri = intent.data

        return Intent.ACTION_VIEW == action && uri.toString().startsWith(JETPACK_CONNECTED_REDIRECT_URL)
    }

    private fun showMainActivityAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun getLoginMode(): LoginMode = LoginMode.WOO_LOGIN_MODE

    override fun startOver() {
        // Clear logged in url from AppPrefs
        AppPrefs.removeLoginSiteAddress()

        val intent = Intent(this, LoginActivity::class.java)
            .apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                LoginMode.WOO_LOGIN_MODE.putInto(this)
            }
        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun loginViaSiteAddress() {
        val loginSiteAddressFragment = supportFragmentManager.findFragmentByTag(LoginSiteAddressFragment.TAG)
            as? PostLoginSitedAddressFragment
            ?: PostLoginSitedAddressFragment()
        changeFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG)
    }

    override fun gotConnectedSiteInfo(siteAddress: String, redirectUrl: String?, hasJetpack: Boolean) {
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = siteAddress.replaceFirst(protocolRegex, "")

        if (hasJetpack) {
            // This most probably means an account mismatch
            // Save the address to allow the site picker to continue the flow
            AppPrefs.setLoginSiteAddress(siteAddressClean)
            showMainActivityAndFinish()
        } else {
            val jetpackReqFragment = LoginJetpackRequiredFragment.newInstance(siteAddress)
            changeFragment(
                fragment = jetpackReqFragment as Fragment,
                shouldAddToBackStack = true,
                tag = LoginJetpackRequiredFragment.TAG
            )
        }
    }

    override fun handleSiteAddressError(siteInfo: ConnectSiteInfoPayload) {
        if (!siteInfo.isWordPress) {
            // The url entered is not a WordPress site.
            val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
            val siteAddressClean = siteInfo.url.replaceFirst(protocolRegex, "")
            val errorMessage = getString(R.string.login_not_wordpress_site_v2)

            // hide the keyboard
            org.wordpress.android.util.ActivityUtils.hideKeyboard(this)

            // show the "not WordPress error" screen
            val genericErrorFragment = LoginSiteCheckErrorFragment.newInstance(siteAddressClean, errorMessage)
            changeFragment(
                fragment = genericErrorFragment,
                shouldAddToBackStack = true,
                tag = LoginSiteCheckErrorFragment.TAG
            )
        }
    }

    override fun helpNoJetpackScreen(
        siteAddress: String?,
        endpointAddress: String?,
        username: String?,
        password: String?,
        userAvatarUrl: String?,
        checkJetpackAvailability: Boolean?
    ) {
        siteAddress?.let {
            val jetpackReqFragment = LoginJetpackRequiredFragment.newInstance(siteAddress)
            changeFragment(
                fragment = jetpackReqFragment as Fragment,
                shouldAddToBackStack = true,
                tag = LoginJetpackRequiredFragment.TAG
            )
        }
    }

    /**
     * This is called when we get a WPCom site without Jetpack (a non-atomic site)
     * We will save the site address then forward to the site picker to show the account mismatch error
     */
    override fun gotWpcomSiteInfo(siteAddress: String?) {
        siteAddress?.let {
            AppPrefs.setLoginSiteAddress(it)
            showMainActivityAndFinish()
        }
    }

    private fun changeFragment(
        fragment: Fragment,
        shouldAddToBackStack: Boolean,
        tag: String,
        animate: Boolean = true
    ) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) {
            fragmentTransaction.setCustomAnimations(
                R.anim.default_enter_anim,
                R.anim.default_exit_anim,
                R.anim.default_pop_enter_anim,
                R.anim.default_pop_exit_anim
            )
        }
        fragmentTransaction.replace(binding.fragmentContainer.id, fragment, tag)
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(tag)
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun handleSslCertificateError(
        memorizingTrustManager: MemorizingTrustManager?,
        callback: SelfSignedSSLCallback?
    ) {
        WooLog.e(WooLog.T.LOGIN, "Self-signed SSL certificate detected - can't proceed with the login.")
    }

    override fun helpSiteAddress(url: String?) {
        val extraSupportTags = arrayListOf(ZendeskExtraTags.connectingJetpack)
        startActivity(HelpActivity.createIntent(this, Origin.LOGIN_SITE_ADDRESS, extraSupportTags))
    }

    override fun startJetpackInstall(siteAddress: String?) {
        siteAddress?.let { address ->
            // Pass the site address in the redirect URL to retrieve it after installation
            val redirectUrl = "$JETPACK_CONNECTED_REDIRECT_URL?$JETPACK_CONNECTED_REDIRECT_URL_QUERY=$siteAddress"
            val url = "$JETPACK_CONNECT_URL?" +
                "url=$address" +
                "&mobile_redirect=$redirectUrl" +
                "&from=mobile"

            // Use the WPComWebView to reduce chances of account mismatch after signing in
            val wpComWebViewFragment = WPComWebViewFragment().apply {
                arguments = WPComWebViewFragmentArgs(url).toBundle()
            }
            changeFragment(wpComWebViewFragment, true, tag = "Tag")
        }
    }

    override fun showJetpackInstructions() {
        ChromeCustomTabUtils.launchUrl(this, AppUrls.JETPACK_INSTRUCTIONS)
    }

    override fun showJetpackTroubleshootingTips() {
        ChromeCustomTabUtils.launchUrl(this, AppUrls.JETPACK_TROUBLESHOOTING)
    }

    override fun showWhatIsJetpackDialog() {
        LoginWhatIsJetpackDialogFragment().show(supportFragmentManager, LoginWhatIsJetpackDialogFragment.TAG)
    }

    override fun gotUnregisteredSocialAccount(
        email: String?,
        displayName: String?,
        idToken: String?,
        photoUrl: String?,
        service: String?
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun alreadyLoggedInWpcom(oldSitesIds: java.util.ArrayList<Int>?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun gotWpcomEmail(email: String?, verifyEmail: Boolean, authOptions: AuthOptions?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun gotUnregisteredEmail(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loginViaSocialAccount(
        email: String?,
        idToken: String?,
        service: String?,
        isPasswordRequired: Boolean
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loggedInViaSocialAccount(oldSiteIds: ArrayList<Int>?, doLoginUpdate: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loginViaWpcomUsernameInstead() {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loginViaSiteCredentials(inputSiteAddress: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpEmailScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpSocialEmailScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun addGoogleLoginFragment(isSignupFromLoginEnabled: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showHelpFindingConnectedEmail() {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun onTermsOfServiceClicked() {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showMagicLinkSentScreen(email: String?, allowPassword: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun usePasswordInstead(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpMagicLinkRequest(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun openEmailClient(isLogin: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpMagicLinkSent(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun forgotPassword(url: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun useMagicLinkInstead(email: String?, verifyEmail: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun needs2fa(email: String?, password: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun needs2faSocial(
        email: String?,
        userId: String?,
        nonceAuthenticator: String?,
        nonceBackup: String?,
        nonceSms: String?
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun needs2faSocialConnect(email: String?, password: String?, idToken: String?, service: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loggedInViaPassword(oldSitesIds: ArrayList<Int>?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpEmailPasswordScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun gotXmlRpcEndpoint(inputSiteAddress: String?, endpointAddress: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpFindingSiteAddress(username: String?, siteStore: SiteStore?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun saveCredentialsInSmartLock(
        username: String?,
        password: String?,
        displayName: String,
        profilePicture: Uri?
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun loggedInViaUsernamePassword(oldSitesIds: ArrayList<Int>?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpUsernamePassword(url: String?, username: String?, isWpcom: Boolean) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpHandleDiscoveryError(
        siteAddress: String?,
        endpointAddress: String?,
        username: String?,
        password: String?,
        userAvatarUrl: String?,
        errorMessage: Int
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun help2FaScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun startPostLoginServices() {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpSignupEmailScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpSignupMagicLinkScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun helpSignupConfirmationScreen(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showSignupMagicLink(email: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showSignupSocial(
        email: String?,
        displayName: String?,
        idToken: String?,
        photoUrl: String?,
        service: String?
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showSignupToLoginMessage() {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showEmailLoginScreen(siteAddress: String?) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }

    override fun showUsernamePasswordScreen(
        siteAddress: String?,
        endpointAddress: String?,
        inputUsername: String?,
        inputPassword: String?
    ) {
        crashLogging.recordException(IllegalStateException("Unhandled state in PostLoginSiteDiscoveryActivity"))
        Toast.makeText(this, R.string.site_discovery_postlogin_failure, Toast.LENGTH_LONG).show()
    }
}
