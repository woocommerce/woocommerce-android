package com.woocommerce.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.woocommerce.android.databinding.ActivityLoginBinding
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewFragmentArgs
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginJetpackRequiredFragment
import com.woocommerce.android.ui.login.LoginNoJetpackListener
import com.woocommerce.android.ui.login.LoginWhatIsJetpackDialogFragment
import com.woocommerce.android.ui.login.overrides.WooLoginSiteAddressFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.login.AuthOptions
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginListener.SelfSignedSSLCallback
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressFragment
import kotlin.text.RegexOption.IGNORE_CASE

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (hasJetpackConnectedIntent()) {
            val siteAddress = intent.data!!.getQueryParameter(JETPACK_CONNECTED_REDIRECT_URL_QUERY)!!
            // Save the site address to be able to continue login from the MainActivity
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

    override fun gotWpcomEmail(email: String?, verifyEmail: Boolean, authOptions: AuthOptions?) {
        TODO("Not yet implemented")
    }

    override fun gotUnregisteredEmail(email: String?) {
        TODO("Not yet implemented")
    }

    override fun gotUnregisteredSocialAccount(
        email: String?,
        displayName: String?,
        idToken: String?,
        photoUrl: String?,
        service: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun loginViaSiteAddress() {
        val loginSiteAddressFragment = supportFragmentManager.findFragmentByTag(LoginSiteAddressFragment.TAG)
            as? WooLoginSiteAddressFragment
            ?: WooLoginSiteAddressFragment()
        changeFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG)
    }

    override fun loginViaSocialAccount(
        email: String?,
        idToken: String?,
        service: String?,
        isPasswordRequired: Boolean
    ) {
        TODO("Not yet implemented")
    }

    override fun loggedInViaSocialAccount(oldSiteIds: ArrayList<Int>?, doLoginUpdate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun loginViaWpcomUsernameInstead() {
        TODO("Not yet implemented")
    }

    override fun loginViaSiteCredentials(inputSiteAddress: String?) {
        TODO("Not yet implemented")
    }

    override fun helpEmailScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun helpSocialEmailScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun addGoogleLoginFragment(isSignupFromLoginEnabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun showHelpFindingConnectedEmail() {
        TODO("Not yet implemented")
    }

    override fun onTermsOfServiceClicked() {
        TODO("Not yet implemented")
    }

    override fun showMagicLinkSentScreen(email: String?, allowPassword: Boolean) {
        TODO("Not yet implemented")
    }

    override fun usePasswordInstead(email: String?) {
        TODO("Not yet implemented")
    }

    override fun helpMagicLinkRequest(email: String?) {
        TODO("Not yet implemented")
    }

    override fun openEmailClient(isLogin: Boolean) {
        TODO("Not yet implemented")
    }

    override fun helpMagicLinkSent(email: String?) {
        TODO("Not yet implemented")
    }

    override fun forgotPassword(url: String?) {
        TODO("Not yet implemented")
    }

    override fun useMagicLinkInstead(email: String?, verifyEmail: Boolean) {
        TODO("Not yet implemented")
    }

    override fun needs2fa(email: String?, password: String?) {
        TODO("Not yet implemented")
    }

    override fun needs2faSocial(
        email: String?,
        userId: String?,
        nonceAuthenticator: String?,
        nonceBackup: String?,
        nonceSms: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun needs2faSocialConnect(email: String?, password: String?, idToken: String?, service: String?) {
        TODO("Not yet implemented")
    }

    override fun loggedInViaPassword(oldSitesIds: ArrayList<Int>?) {
        TODO("Not yet implemented")
    }

    override fun helpEmailPasswordScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun alreadyLoggedInWpcom(oldSitesIds: ArrayList<Int>?) {
        TODO("Not yet implemented")
    }

    override fun gotWpcomSiteInfo(siteAddress: String?) {
        TODO("Not yet implemented")
    }

    override fun gotConnectedSiteInfo(siteAddress: String, redirectUrl: String?, hasJetpack: Boolean) {
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = siteAddress.replaceFirst(protocolRegex, "")

        if (hasJetpack) {
            // Save site address to continue login in MainActivity
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

    override fun gotXmlRpcEndpoint(inputSiteAddress: String?, endpointAddress: String?) {
        TODO("Not yet implemented")
    }

    override fun handleSslCertificateError(
        memorizingTrustManager: MemorizingTrustManager?,
        callback: SelfSignedSSLCallback?
    ) {
        TODO("Not yet implemented")
    }

    override fun helpSiteAddress(url: String?) {
        TODO("Not yet implemented")
    }

    override fun helpFindingSiteAddress(username: String?, siteStore: SiteStore?) {
        TODO("Not yet implemented")
    }

    override fun handleSiteAddressError(siteInfo: ConnectSiteInfoPayload?) {
        TODO("Not yet implemented")
    }

    override fun saveCredentialsInSmartLock(
        username: String?,
        password: String?,
        displayName: String,
        profilePicture: Uri?
    ) {
        TODO("Not yet implemented")
    }

    override fun loggedInViaUsernamePassword(oldSitesIds: ArrayList<Int>?) {
        TODO("Not yet implemented")
    }

    override fun helpUsernamePassword(url: String?, username: String?, isWpcom: Boolean) {
        TODO("Not yet implemented")
    }

    override fun helpNoJetpackScreen(
        siteAddress: String?,
        endpointAddress: String?,
        username: String?,
        password: String?,
        userAvatarUrl: String?,
        checkJetpackAvailability: Boolean?
    ) {
        TODO("Not yet implemented")
    }

    override fun helpHandleDiscoveryError(
        siteAddress: String?,
        endpointAddress: String?,
        username: String?,
        password: String?,
        userAvatarUrl: String?,
        errorMessage: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun help2FaScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun startPostLoginServices() {
        TODO("Not yet implemented")
    }

    override fun helpSignupEmailScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun helpSignupMagicLinkScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun helpSignupConfirmationScreen(email: String?) {
        TODO("Not yet implemented")
    }

    override fun showSignupMagicLink(email: String?) {
        TODO("Not yet implemented")
    }

    override fun showSignupSocial(
        email: String?,
        displayName: String?,
        idToken: String?,
        photoUrl: String?,
        service: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun showSignupToLoginMessage() {
        TODO("Not yet implemented")
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

    override fun showEmailLoginScreen(siteAddress: String?) {
        TODO("Not yet implemented")
    }

    override fun showUsernamePasswordScreen(
        siteAddress: String?,
        endpointAddress: String?,
        inputUsername: String?,
        inputPassword: String?
    ) {
        TODO("Not yet implemented")
    }

    override fun startJetpackInstall(siteAddress: String?) {
        siteAddress?.let { address ->
            // Pass the site address in the redirect URL to retrieve it after installation
            val redirectUrl = "$JETPACK_CONNECTED_REDIRECT_URL?$JETPACK_CONNECTED_REDIRECT_URL_QUERY=$siteAddress"
            val url = "$JETPACK_CONNECT_URL?" +
                "url=$address" +
                "&mobile_redirect=$redirectUrl" +
                "&from=mobile"

            val wpComWebViewFragment = WPComWebViewFragment().apply {
                arguments = WPComWebViewFragmentArgs(url).toBundle()
            }
            changeFragment(wpComWebViewFragment, true, tag = "Tag")
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
}
