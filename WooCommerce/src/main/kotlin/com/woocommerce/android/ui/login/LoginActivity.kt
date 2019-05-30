package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.automattic.android.tracks.CrashLogging.CrashLogging
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.support.ZendeskExtraTags
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.ui.login.LoginPrologueFragment.PrologueFinishedListener
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
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

class LoginActivity : AppCompatActivity(), LoginListener, GoogleListener, PrologueFinishedListener,
        HasSupportFragmentInjector {
    companion object {
        private const val FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword"
    }

    @Inject internal lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject internal lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject internal lateinit var zendeskHelper: ZendeskHelper
    @Inject lateinit var supportHelper: SupportHelper

    private var loginMode: LoginMode? = null

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        if (savedInstanceState == null) {
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

    override fun onPrologueFinished() {
        // TODO Check loginMode here and handle different login cases
        startLogin()
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

    private fun getLoginEmailFragment(): LoginEmailFragment? {
        val fragment = supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG)
        return if (fragment == null) null else fragment as LoginEmailFragment
    }

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

    private fun startLogin() {
        if (getLoginEmailFragment() != null) {
            // email screen is already shown so, login has already started. Just bail.
            return
        }

        slideInFragment(LoginEmailFragment(), true, LoginEmailFragment.TAG)
    }

    //  -- BEGIN: LoginListener implementation methods

    override fun gotWpcomEmail(email: String?) {
        if (getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT) {
            val loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email,
                    AuthEmailPayloadScheme.WOOCOMMERCE, false, null)
            slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG)
        } else {
            val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, null, null, false)
            slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
        }
    }

    override fun loginViaSiteAddress() {
        val loginSiteAddressFragment = LoginSiteAddressFragment()
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
        val loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                siteAddress, siteAddress, siteName, siteIconUrl, null, null, true)
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG)
    }

    override fun gotXmlRpcEndpoint(inputSiteAddress: String?, endpointAddress: String?) {
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
        // TODO: Signup
    }

    //  -- END: GoogleListener implementation methods
}
