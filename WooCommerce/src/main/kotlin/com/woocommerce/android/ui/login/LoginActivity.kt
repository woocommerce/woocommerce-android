package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.GoogleFragment.GoogleListener
import org.wordpress.android.login.Login2FaFragment
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

class LoginActivity : AppCompatActivity(), LoginListener, GoogleListener, HasSupportFragmentInjector {
    companion object {
        private const val FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword"
    }

    @Inject internal lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    private var loginMode: LoginMode? = null

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED)
            // TODO Check loginMode here and handle different login cases
            startLogin()
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        fragmentTransaction.commit()
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

    override fun getLoginMode(): LoginMode {
        if (loginMode != null) {
            // returned the cached value
            return loginMode as LoginMode
        }

        // compute and cache the Login mode
        loginMode = LoginMode.fromIntent(intent)

        return loginMode as LoginMode
    }

    private fun loggedInAndFinish() {
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

        showFragment(LoginEmailFragment(), LoginEmailFragment.TAG)
    }

    //  -- BEGIN: LoginListener implementation methods

    override fun gotWpcomEmail(email: String?) {
        if (getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT) {
            val loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email)
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

    override fun loginViaSocialAccount(email: String?, idToken: String?, service: String?,
                                       isPasswordRequired: Boolean) {
        val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, idToken,
                service, isPasswordRequired)
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun loggedInViaSocialAccount(oldSitesIds: ArrayList<Int>, doLoginUpdate: Boolean) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_SUCCESS)
        loggedInAndFinish()
    }

    override fun loginViaWpcomUsernameInstead() {
        jumpToUsernamePassword(null, null)
    }

    override fun showMagicLinkSentScreen(email: String?) {
        val loginMagicLinkSentFragment = LoginMagicLinkSentFragment.newInstance(email)
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG)
    }

    override fun openEmailClient() {
        if (ActivityUtils.isEmailClientAvailable(this)) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED)
            ActivityUtils.openEmailClient(this)
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found)
        }
    }

    override fun usePasswordInstead(email: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED)
        val loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null, null, null, false)
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun forgotPassword(url: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FORGOT_PASSWORD_CLICKED)
        ActivityUtils.openUrlExternal(this, url + FORGOT_PASSWORD_URL_SUFFIX)
    }

    override fun needs2fa(email: String?, password: String?) {
        val login2FaFragment = Login2FaFragment.newInstance(email, password)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocial(email: String?, userId: String?, nonceAuthenticator: String?, nonceBackup: String?,
                                nonceSms: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_2FA_NEEDED)
        val login2FaFragment = Login2FaFragment.newInstanceSocial(email, userId,
                nonceAuthenticator, nonceBackup, nonceSms)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocialConnect(email: String?, password: String?, idToken: String?, service: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_2FA_NEEDED)
        val login2FaFragment = Login2FaFragment.newInstanceSocialConnect(email, password, idToken, service)
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun loggedInViaPassword(oldSitesIds: ArrayList<Int>) {
        loggedInAndFinish()
    }

    override fun alreadyLoggedInWpcom(oldSitesIds: ArrayList<Int>) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG)
        loggedInAndFinish()
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

    override fun handleSslCertificateError(memorizingTrustManager: MemorizingTrustManager?,
                                           callback: LoginListener.SelfSignedSSLCallback?) {
        // TODO: Support self-signed SSL sites and show dialog (only needed when XML-RPC support is added)
    }

    override fun helpSiteAddress(url: String?) {
        // TODO: Helpshift support
//        launchHelpshift(url, null, false, Tag.ORIGIN_LOGIN_SITE_ADDRESS)
    }

    override fun helpFindingSiteAddress(username: String?, siteStore: SiteStore?) {
        // TODO: Helpshift support
//        HelpshiftHelper.getInstance().showConversation(this, siteStore, Tag.ORIGIN_LOGIN_SITE_ADDRESS, username)
    }

    // TODO This can be modified to also receive the URL the user entered, so we can make that the primary store
    override fun loggedInViaUsernamePassword(oldSitesIds: ArrayList<Int>) {
        loggedInAndFinish()
    }

    override fun helpEmailScreen(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_EMAIL)
    }

    override fun helpSocialEmailScreen(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_SOCIAL)
    }

    override fun addGoogleLoginFragment(parent: Fragment) {
        // TODO: Remove this toast when social signin with Google is configured
        ToastUtils.showToast(this, "Login with Google is not yet implemented")
        val fragmentManager = parent.childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        var loginGoogleFragment = fragmentManager.findFragmentByTag(LoginGoogleFragment.TAG) as LoginGoogleFragment?

        if (loginGoogleFragment != null) {
            fragmentTransaction.remove(loginGoogleFragment)
        }

        loginGoogleFragment = LoginGoogleFragment()
        loginGoogleFragment.retainInstance = true
        fragmentTransaction.add(loginGoogleFragment, LoginGoogleFragment.TAG)
        fragmentTransaction.commit()
    }

    override fun helpMagicLinkRequest(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_MAGIC_LINK)
    }

    override fun helpMagicLinkSent(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_MAGIC_LINK)
    }

    override fun helpEmailPasswordScreen(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_EMAIL_PASSWORD)
    }

    override fun help2FaScreen(email: String?) {
        // TODO: Helpshift support
//        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_2FA)
    }

    override fun startPostLoginServices() {
        // TODO Start future NotificationsUpdateService
    }

    override fun helpUsernamePassword(url: String?, username: String?, isWpcom: Boolean) {
        // TODO: Helpshift support
//        launchHelpshift(url, username, isWpcom, Tag.ORIGIN_LOGIN_USERNAME_PASSWORD)
    }

    override fun setHelpContext(faqId: String?, faqSection: String?) {
        // nothing implemented here yet. This will set the context the `help()` callback should work with
    }

    // SmartLock

    override fun saveCredentialsInSmartLock(username: String?, password: String?, displayName: String,
                                            profilePicture: Uri?) {
        // TODO: Hook for smartlock, if using
    }

    // Signup

    override fun helpSignupEmailScreen(email: String?) {
        // TODO: Signup
    }

    override fun helpSignupMagicLinkScreen(email: String?) {
        // TODO: Signup
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
        val loginEmailFragment = supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG) as LoginEmailFragment
        loginEmailFragment.setGoogleEmail(email)
    }

    override fun onGoogleLoginFinished() {
        val loginEmailFragment = supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG) as LoginEmailFragment
        loginEmailFragment.finishLogin()
    }

    override fun onGoogleSignupFinished(name: String?, email: String?, photoUrl: String?, username: String?) {
        // TODO: Signup
    }

    //  -- END: GoogleListener implementation methods
}
