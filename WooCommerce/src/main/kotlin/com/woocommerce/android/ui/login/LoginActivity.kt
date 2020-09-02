package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.automattic.android.tracks.CrashLogging.CrashLogging
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.ZendeskExtraTags
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.ui.login.LoginPrologueFragment.PrologueFinishedListener
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.activity_login.*
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.GoogleFragment.GoogleListener
import org.wordpress.android.login.Login2FaFragment
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginEmailFragment
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginGoogleFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginMagicLinkRequestFragment
import org.wordpress.android.login.LoginMagicLinkSentFragment
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressFragment
import org.wordpress.android.login.LoginUsernamePasswordFragment
import org.wordpress.android.util.ToastUtils
import java.util.ArrayList
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

@Suppress("SameParameterValue")
class LoginActivity : AppCompatActivity(), LoginListener, GoogleListener, PrologueFinishedListener,
        HasAndroidInjector, LoginNoJetpackListener, LoginEmailHelpDialogFragment.Listener {
    companion object {
        private const val FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword"
        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"
    }

    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject internal lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject internal lateinit var zendeskHelper: ZendeskHelper

    private var loginMode: LoginMode? = null

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        if (hasMagicLinkLoginIntent()) {
            getAuthTokenFromIntent()?.let { showMagicLinkInterceptFragment(it) }
        } else if (savedInstanceState == null) {
            loginAnalyticsListener.trackLoginAccessed()
            showPrologueFragment()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun showPrologueFragment() {
        val fragment = LoginPrologueFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, LoginPrologueFragment.TAG)
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    private fun hasMagicLinkLoginIntent(): Boolean {
        val action = intent.action
        val uri = intent.data
        val host = uri?.host?.let { it } ?: ""
        return Intent.ACTION_VIEW == action && host.contains(MAGIC_LOGIN)
    }

    private fun getAuthTokenFromIntent(): String? {
        val uri = intent.data
        return uri?.getQueryParameter(TOKEN_PARAMETER)
    }

    private fun showMagicLinkInterceptFragment(authToken: String) {
        val fragment = MagicLinkInterceptFragment.newInstance(authToken)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, LoginPrologueFragment.TAG)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    private fun slideInFragment(fragment: Fragment, shouldAddToBackStack: Boolean, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
                R.anim.activity_slide_in_from_right,
                R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left,
                R.anim.activity_slide_out_to_right)
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null)
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    /**
     * The normal layout for the login library will include social login but
     * there is an alternative layout used specifically for logging in using the
     * site address flow. This layout includes an option to sign in with site
     * credentials.
     *
     * @param useAltLayout If true, use the layout that includes the option to log
     * in with site credentials.
     */
    private fun getLoginEmailFragment(useAltLayout: Boolean): LoginEmailFragment? {
        val fragment = if (useAltLayout) {
            supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG_ALT_LAYOUT)
        } else {
            supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG)
        }
        return if (fragment == null) null else fragment as LoginEmailFragment
    }

    private fun getLoginViaSiteAddressFragment(): LoginSiteAddressFragment? =
            supportFragmentManager.findFragmentByTag(LoginSiteAddressFragment.TAG) as? LoginSiteAddressFragment

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun getLoginMode(): LoginMode {
        if (loginMode != null) {
            // returned the cached value
            return loginMode as LoginMode
        }

        // compute and cache the Login mode
        loginMode = LoginMode.fromIntent(intent)

        return loginMode as LoginMode
    }

    override fun startOver() {
        showPrologueFragment()
    }

    override fun onPrimaryButtonClicked() {
        loginViaSiteAddress()
    }

    override fun onSecondaryButtonClicked() {
        startLoginViaWPCom()
    }

    private fun showMainActivityAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun jumpToUsernamePassword(username: String?, password: String?) {
        val loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                "wordpress.com", "wordpress.com", "WordPress.com", "https://s0.wp.com/i/webclip.png", username,
                password, true)
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG)
    }

    private fun startLoginViaWPCom() {
        if (getLoginEmailFragment(useAltLayout = false) != null) {
            // login by wpcom is already shown so login has already started. Just bail.
            return
        }

        showEmailLoginScreen()
    }

    //  -- BEGIN: LoginListener implementation methods

    override fun gotWpcomEmail(email: String?, verifyEmail: Boolean) {
        if (getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT) {
            val loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email,
                    AuthEmailPayloadScheme.WOOCOMMERCE, false, null, verifyEmail)
            slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG)
        } else {
            val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, null, null, false)
            slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
        }
    }

    override fun loginViaSiteAddress() {
        val loginSiteAddressFragment = getLoginViaSiteAddressFragment() ?: LoginSiteAddressFragment()
        slideInFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG)
    }

    override fun loginViaSocialAccount(
        email: String?,
        idToken: String?,
        service: String?,
        isPasswordRequired: Boolean
    ) {
        val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, idToken,
                service, isPasswordRequired)
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun loggedInViaSocialAccount(oldSitesIds: ArrayList<Int>, doLoginUpdate: Boolean) {
        loginAnalyticsListener.trackLoginSocialSuccess()
        CrashLogging.setNeedsDataRefresh()
        showMainActivityAndFinish()
    }

    override fun loginViaWpcomUsernameInstead() {
        jumpToUsernamePassword(null, null)
    }

    override fun showMagicLinkSentScreen(email: String?) {
        val loginMagicLinkSentFragment = LoginMagicLinkSentFragment.newInstance(email)
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG)
    }

    override fun openEmailClient(isLogin: Boolean) {
        if (ActivityUtils.isEmailClientAvailable(this)) {
            loginAnalyticsListener.trackLoginMagicLinkOpenEmailClientClicked()
            ActivityUtils.openEmailClient(this)
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found)
        }
    }

    override fun usePasswordInstead(email: String?) {
        loginAnalyticsListener.trackLoginMagicLinkExited()
        val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, null, null, false)
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun forgotPassword(url: String?) {
        loginAnalyticsListener.trackLoginForgotPasswordClicked()
        ChromeCustomTabUtils.launchUrl(this, url + FORGOT_PASSWORD_URL_SUFFIX)
    }

    override fun needs2fa(email: String?, password: String?) {
        val login2FaFragment = Login2FaFragment.newInstance(email, password)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocial(
        email: String?,
        userId: String?,
        nonceAuthenticator: String?,
        nonceBackup: String?,
        nonceSms: String?
    ) {
        loginAnalyticsListener.trackLoginSocial2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstanceSocial(email, userId,
                nonceAuthenticator, nonceBackup, nonceSms)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocialConnect(email: String?, password: String?, idToken: String?, service: String?) {
        loginAnalyticsListener.trackLoginSocial2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstanceSocialConnect(email, password, idToken, service)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun loggedInViaPassword(oldSitesIds: ArrayList<Int>) {
        CrashLogging.setNeedsDataRefresh()
        showMainActivityAndFinish()
    }

    override fun alreadyLoggedInWpcom(oldSitesIds: ArrayList<Int>) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG)
        showMainActivityAndFinish()
    }

    override fun gotWpcomSiteInfo(siteAddress: String?, siteName: String?, siteIconUrl: String?) {
        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app.
        siteAddress?.let { AppPrefs.setLoginSiteAddress(it) }
        showEmailLoginScreen(siteAddress)
    }

    override fun gotConnectedSiteInfo(siteAddress: String, redirectUrl: String?, hasJetpack: Boolean) {
        // If the redirect url is available, use that as the preferred url. Pass this url to the other fragments
        // with the protocol since it is needed for initiating forgot password flow etc in the login process.
        val inputSiteAddress = redirectUrl ?: siteAddress

        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app. Strip the protocol from this url string prior to saving to AppPrefs since it's
        // not needed and may cause issues when attempting to match the url to the authenticated account later
        // in the login process.
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = inputSiteAddress.replaceFirst(protocolRegex, "")
        AppPrefs.setLoginSiteAddress(siteAddressClean)

        if (hasJetpack) {
            showEmailLoginScreen(inputSiteAddress)
        } else {
            // hide the keyboard
            org.wordpress.android.util.ActivityUtils.hideKeyboard(this)

            // Show the 'Jetpack required' fragment
            val jetpackReqFragment = LoginJetpackRequiredFragment.newInstance(siteAddressClean)
            slideInFragment(
                    fragment = jetpackReqFragment as Fragment,
                    shouldAddToBackStack = true,
                    tag = LoginJetpackRequiredFragment.TAG)
        }
    }

    /**
     * Method called when Login with Site credentials link is clicked in the [LoginEmailFragment]
     * This method is called instead of [LoginListener.gotXmlRpcEndpoint] since calling that method overrides
     * the already saved [inputSiteAddress] without the protocol, with the same site address but with
     * the protocol. This may cause issues when attempting to match the url to the authenticated account later
     * in the login process.
     */
    override fun loginViaSiteCredentials(inputSiteAddress: String?) {
        showUsernamePasswordScreen(inputSiteAddress, null, null, null)
    }

    override fun gotXmlRpcEndpoint(inputSiteAddress: String?, endpointAddress: String?) {
        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app.
        inputSiteAddress?.let { AppPrefs.setLoginSiteAddress(it) }

        val loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                inputSiteAddress, endpointAddress, null, null, null, null, false)
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG)
    }

    override fun handleSslCertificateError(
        memorizingTrustManager: MemorizingTrustManager?,
        callback: LoginListener.SelfSignedSSLCallback?
    ) {
        // TODO: Support self-signed SSL sites and show dialog (only needed when XML-RPC support is added)
    }

    private fun viewHelpAndSupport(origin: Origin) {
        val extraSupportTags = arrayListOf(ZendeskExtraTags.connectingJetpack)
        startActivity(HelpActivity.createIntent(this, origin, extraSupportTags))
    }

    override fun helpSiteAddress(url: String?) {
        viewHelpAndSupport(Origin.LOGIN_SITE_ADDRESS)
    }

    override fun helpFindingSiteAddress(username: String?, siteStore: SiteStore?) {
        zendeskHelper.createNewTicket(this, Origin.LOGIN_SITE_ADDRESS, null)
    }

    // TODO This can be modified to also receive the URL the user entered, so we can make that the primary store
    override fun loggedInViaUsernamePassword(oldSitesIds: ArrayList<Int>) {
        CrashLogging.setNeedsDataRefresh()
        showMainActivityAndFinish()
    }

    override fun helpEmailScreen(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_EMAIL)
    }

    override fun helpSocialEmailScreen(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_SOCIAL)
    }

    override fun addGoogleLoginFragment() {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val loginGoogleFragment = LoginGoogleFragment().apply {
            retainInstance = true
        }
        fragmentTransaction.add(loginGoogleFragment, LoginGoogleFragment.TAG)
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun helpMagicLinkRequest(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_MAGIC_LINK)
    }

    override fun helpMagicLinkSent(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_MAGIC_LINK)
    }

    override fun helpEmailPasswordScreen(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_EMAIL_PASSWORD)
    }

    override fun help2FaScreen(email: String?) {
        viewHelpAndSupport(Origin.LOGIN_2FA)
    }

    override fun startPostLoginServices() {
        // TODO Start future NotificationsUpdateService
    }

    override fun helpUsernamePassword(url: String?, username: String?, isWpcom: Boolean) {
        viewHelpAndSupport(Origin.LOGIN_USERNAME_PASSWORD)
    }

    override fun helpNoJetpackScreen(
        siteAddress: String,
        endpointAddress: String?,
        username: String,
        password: String,
        userAvatarUrl: String?,
        checkJetpackAvailability: Boolean
    ) {
        val jetpackReqFragment = LoginNoJetpackFragment.newInstance(
                siteAddress, endpointAddress, username, password, userAvatarUrl,
                checkJetpackAvailability
        )
        slideInFragment(
                fragment = jetpackReqFragment as Fragment,
                shouldAddToBackStack = true,
                tag = LoginJetpackRequiredFragment.TAG)
    }

    override fun helpHandleDiscoveryError(
        siteAddress: String,
        endpointAddress: String?,
        username: String,
        password: String,
        userAvatarUrl: String?,
        errorMessage: Int
    ) {
        val discoveryErrorFragment = LoginDiscoveryErrorFragment.newInstance(
                siteAddress, endpointAddress, username, password, userAvatarUrl, errorMessage
        )
        slideInFragment(
                fragment = discoveryErrorFragment as Fragment,
                shouldAddToBackStack = true,
                tag = LoginJetpackRequiredFragment.TAG)
    }

    // SmartLock

    override fun saveCredentialsInSmartLock(
        username: String?,
        password: String?,
        displayName: String,
        profilePicture: Uri?
    ) {
        // TODO: Hook for smartlock, if using
    }

    // Signup

    override fun doStartSignup() {
        // TODO: Signup
    }

    override fun helpSignupEmailScreen(email: String?) {
        viewHelpAndSupport(Origin.SIGNUP_EMAIL)
    }

    override fun helpSignupMagicLinkScreen(email: String?) {
        viewHelpAndSupport(Origin.SIGNUP_MAGIC_LINK)
    }

    override fun showSignupMagicLink(email: String?) {
        // TODO: Signup
    }

    override fun showSignupToLoginMessage() {
        // TODO: Signup
    }

    //  -- END: LoginListener implementation methods

    //  -- BEGIN: GoogleListener implementation methods

    override fun onGoogleEmailSelected(email: String?) {
        (supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG) as? LoginEmailFragment)?.setGoogleEmail(email)
    }

    override fun onGoogleLoginFinished() {
        (supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG) as? LoginEmailFragment)?.finishLogin()
    }

    override fun onGoogleSignupFinished(name: String?, email: String?, photoUrl: String?, username: String?) {
        // TODO: Signup
    }

    override fun onGoogleSignupError(msg: String?) {
        Snackbar.make(main_view, msg ?: "", BaseTransientBottomBar.LENGTH_LONG).show()
    }

    //  -- END: GoogleListener implementation methods

    override fun showJetpackInstructions() {
        ChromeCustomTabUtils.launchUrl(this, AppUrls.JETPACK_INSTRUCTIONS)
    }

    override fun showJetpackTroubleshootingTips() {
        ChromeCustomTabUtils.launchUrl(this, AppUrls.JETPACK_TROUBLESHOOTING)
    }

    override fun showWhatIsJetpackDialog() {
        LoginWhatIsJetpackDialogFragment().show(supportFragmentManager, LoginWhatIsJetpackDialogFragment.TAG)
    }

    override fun showHelpFindingConnectedEmail() {
        AnalyticsTracker.track(Stat.LOGIN_BY_EMAIL_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED)

        LoginEmailHelpDialogFragment().show(supportFragmentManager, LoginEmailHelpDialogFragment.TAG)
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(HelpActivity.createIntent(this, Origin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }

    override fun showEmailLoginScreen(siteAddress: String?) {
        if (siteAddress != null) {
            val loginEmailFragment = getLoginEmailFragment(
                useAltLayout = false) ?: LoginEmailFragment.newInstance(siteAddress, true)
            slideInFragment(loginEmailFragment as Fragment, true, LoginEmailFragment.TAG)
        } else {
            val loginEmailFragment = getLoginEmailFragment(
                useAltLayout = true) ?: LoginEmailFragment.newInstance(false, false, true)
            slideInFragment(
                loginEmailFragment as Fragment, true, LoginEmailFragment.TAG_ALT_LAYOUT)
        }
    }

    override fun showUsernamePasswordScreen(
        siteAddress: String?,
        endpointAddress: String?,
        inputUsername: String?,
        inputPassword: String?
    ) {
        val loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                siteAddress, endpointAddress, null, null, inputUsername, inputPassword,
                false)
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG)
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

    override fun helpSignupConfirmationScreen(email: String?) {
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
}
