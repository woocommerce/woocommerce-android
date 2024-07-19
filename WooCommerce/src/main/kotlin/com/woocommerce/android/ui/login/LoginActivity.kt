package com.woocommerce.android.ui.login

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.AppUrls
import com.woocommerce.android.AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_URL
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_JETPACK_INSTALLATION_SOURCE_WEB
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_LOGIN_WITH_WORDPRESS_COM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_NO_WP_COM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_WP_COM
import com.woocommerce.android.analytics.ExperimentTracker
import com.woocommerce.android.databinding.ActivityLoginBinding
import com.woocommerce.android.extensions.parcelable
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.support.requests.SupportRequestFormActivity
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.login.LoginPrologueCarouselFragment.PrologueCarouselListener
import com.woocommerce.android.ui.login.LoginPrologueFragment.PrologueFinishedListener
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow.LOGIN_SITE_ADDRESS
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Source
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step.ENTER_SITE_ADDRESS
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorFragment
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorFragmentArgs
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchErrorType
import com.woocommerce.android.ui.login.accountmismatch.AccountMismatchErrorViewModel.AccountMismatchPrimaryButton
import com.woocommerce.android.ui.login.error.LoginNoWPcomAccountFoundDialogFragment
import com.woocommerce.android.ui.login.error.LoginNotWPDialogFragment
import com.woocommerce.android.ui.login.overrides.WooLoginEmailFragment
import com.woocommerce.android.ui.login.overrides.WooLoginEmailPasswordFragment
import com.woocommerce.android.ui.login.overrides.WooLoginSiteAddressFragment
import com.woocommerce.android.ui.login.qrcode.QrCodeLoginListener
import com.woocommerce.android.ui.login.qrcode.ValidateScannedValue
import com.woocommerce.android.ui.login.sitecredentials.LoginSiteCredentialsFragment
import com.woocommerce.android.ui.login.sitecredentials.applicationpassword.ApplicationPasswordTutorialFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.ChromeCustomTabUtils.Height.Partial.ThreeQuarters
import com.woocommerce.android.util.UrlUtils
import com.woocommerce.android.util.WooLog
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme.WOOCOMMERCE
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.OnConnectSiteInfoChecked
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import org.wordpress.android.login.AuthOptions
import org.wordpress.android.login.GoogleFragment.GoogleListener
import org.wordpress.android.login.Login2FaFragment
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginEmailFragment
import org.wordpress.android.login.LoginEmailPasswordFragment
import org.wordpress.android.login.LoginGoogleFragment
import org.wordpress.android.login.LoginListener
import org.wordpress.android.login.LoginMagicLinkRequestFragment
import org.wordpress.android.login.LoginMode
import org.wordpress.android.login.LoginSiteAddressFragment
import org.wordpress.android.login.LoginUsernamePasswordFragment
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

// TODO Extract logic out of LoginActivity to reduce size
@Suppress("SameParameterValue", "LargeClass")
@AndroidEntryPoint
class LoginActivity :
    AppCompatActivity(),
    LoginListener,
    GoogleListener,
    PrologueFinishedListener,
    PrologueCarouselListener,
    HasAndroidInjector,
    LoginNoJetpackListener,
    LoginEmailHelpDialogFragment.Listener,
    WooLoginEmailFragment.Listener,
    LoginSiteCredentialsFragment.Listener,
    QrCodeLoginListener {
    companion object {
        private const val FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword"
        private const val JETPACK_CONNECT_URL = "https://wordpress.com/jetpack/connect"
        private const val JETPACK_CONNECTED_REDIRECT_URL = "woocommerce://jetpack-connected"
        private const val APPLICATION_PASSWORD_LOGIN_ZENDESK_TAG = "application_password_login_error"

        private const val KEY_UNIFIED_TRACKER_SOURCE = "KEY_UNIFIED_TRACKER_SOURCE"
        private const val KEY_UNIFIED_TRACKER_FLOW = "KEY_UNIFIED_TRACKER_FLOW"
        private const val KEY_CONNECT_SITE_INFO = "KEY_CONNECT_SITE_INFO"

        const val LOGIN_WITH_WPCOM_EMAIL_ACTION = "login_with_wpcom_email"
        const val EMAIL_PARAMETER = "email"

        const val SITE_URL_PARAMETER = "siteUrl"
        const val WP_COM_EMAIL_PARAMETER = "wpcomEmail"
        const val APP_LOGIN_AUTHORITY = "app-login"
        const val USERNAME_PARAMETER = "username"
    }

    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject internal lateinit var loginAnalyticsListener: LoginAnalyticsListener

    @Inject internal lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject internal lateinit var urlUtils: UrlUtils

    @Inject internal lateinit var experimentTracker: ExperimentTracker

    @Inject internal lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject internal lateinit var dispatcher: Dispatcher

    @Inject internal lateinit var uiMessageResolver: UIMessageResolver

    private var loginMode: LoginMode? = null
    private lateinit var binding: ActivityLoginBinding

    private var connectSiteInfo: ConnectSiteInfo? = null

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChromeCustomTabUtils.registerForPartialTabUsage(this)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )

        dispatcher.register(this)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        when {
            intent?.action == LOGIN_WITH_WPCOM_EMAIL_ACTION -> {
                val email = intent.extras!!.getString(EMAIL_PARAMETER)
                gotWpcomEmail(email, verifyEmail = true, null)
            }

            intent?.action == Intent.ACTION_VIEW && intent.data?.authority == APP_LOGIN_AUTHORITY -> {
                intent.data?.let { uri -> handleAppLoginUri(uri) }
            }

            hasJetpackConnectedIntent() -> {
                AnalyticsTracker.track(
                    stat = AnalyticsEvent.LOGIN_JETPACK_SETUP_COMPLETED,
                    properties = mapOf(KEY_SOURCE to VALUE_JETPACK_INSTALLATION_SOURCE_WEB)
                )
                startLoginViaWPCom()
            }

            savedInstanceState == null -> {
                loginAnalyticsListener.trackLoginAccessed()
                showPrologue()
            }
        }

        savedInstanceState?.let { ss ->
            unifiedLoginTracker.setSource(ss.getString(KEY_UNIFIED_TRACKER_SOURCE, Source.DEFAULT.value))
            unifiedLoginTracker.setFlow(ss.getString(KEY_UNIFIED_TRACKER_FLOW))
            connectSiteInfo = ss.parcelable(KEY_CONNECT_SITE_INFO)
        }
    }

    private fun handleBackPress() {
        AnalyticsTracker.trackBackPressed(this)
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        dispatcher.unregister(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(KEY_UNIFIED_TRACKER_SOURCE, unifiedLoginTracker.getSource().value)
        outState.putParcelable(KEY_CONNECT_SITE_INFO, connectSiteInfo)
        unifiedLoginTracker.getFlow()?.value?.let {
            outState.putString(KEY_UNIFIED_TRACKER_FLOW, it)
        }
    }

    private fun showPrologue() {
        if (!appPrefsWrapper.hasOnboardingCarouselBeenDisplayed()) {
            showPrologueCarouselFragment()
        } else {
            showPrologueFragment()
        }
    }

    private fun hasJetpackConnectedIntent(): Boolean {
        val action = intent.action
        val uri = intent.data

        return Intent.ACTION_VIEW == action && uri.toString() == JETPACK_CONNECTED_REDIRECT_URL
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
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(tag)
            fragmentTransaction.setPrimaryNavigationFragment(fragment)
        }
        fragmentTransaction.commitAllowingStateLoss()
    }

    /**
     * The normal layout for the login library will include social login but
     * there is an alternative layout used specifically for logging in using the
     * site address flow. This layout includes an option to sign in with site
     * credentials.
     *
     * @param siteCredsLayout If true, use the layout that includes the option to log
     * in with site credentials.
     */
    private fun getLoginEmailFragment(siteCredsLayout: Boolean): LoginEmailFragment? {
        val fragment = if (siteCredsLayout) {
            supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG_SITE_CREDS_LAYOUT)
        } else {
            supportFragmentManager.findFragmentByTag(LoginEmailFragment.TAG)
        }
        return if (fragment == null) null else fragment as LoginEmailFragment
    }

    private fun getPrologueFragment(): LoginPrologueFragment? =
        supportFragmentManager.findFragmentByTag(LoginPrologueFragment.TAG) as? LoginPrologueFragment

    private fun getLoginViaSiteAddressFragment(): LoginSiteAddressFragment? =
        supportFragmentManager.findFragmentByTag(LoginSiteAddressFragment.TAG) as? WooLoginSiteAddressFragment

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
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

    override fun startOver() {
        // Clear logged in url from AppPrefs
        appPrefsWrapper.removeLoginSiteAddress()
        // Pop all the fragments from the backstack until we get to the Prologue fragment
        supportFragmentManager.popBackStack(LoginPrologueFragment.TAG, 0)
    }

    override fun onPrimaryButtonClicked() {
        unifiedLoginTracker.trackClick(Click.LOGIN_WITH_SITE_ADDRESS)
        loginViaSiteAddress()
    }

    override fun onSecondaryButtonClicked() {
        unifiedLoginTracker.trackClick(Click.CONTINUE_WITH_WORDPRESS_COM)
        startLoginViaWPCom()
    }

    override fun onNewToWooButtonClicked() {
        ChromeCustomTabUtils.launchUrl(this, AppUrls.HOSTING_OPTIONS_DOC)
    }

    private fun showMainActivityAndFinish() {
        experimentTracker.log(ExperimentTracker.LOGIN_SUCCESSFUL_EVENT)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun startLoginViaWPCom() {
        // Clean previously saved site address, e.g: if merchants return from a store address flow.
        appPrefsWrapper.removeLoginSiteAddress()
        unifiedLoginTracker.setFlow(Flow.WORDPRESS_COM.value)
        showEmailLoginScreen()
    }

    override fun gotWpcomEmail(email: String?, verifyEmail: Boolean, authOptions: AuthOptions?) {
        val isMagicLinkEnabled =
            getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT
        email?.let { appPrefsWrapper.setLoginEmail(it) }
        clearCachedSites()

        if (authOptions != null) {
            if (authOptions.isPasswordless) {
                showMagicLinkRequestScreen(email, verifyEmail, allowPassword = false, forceRequestAtStart = true)
            } else {
                showEmailPasswordScreen(email, verifyEmail)
            }
        } else {
            if (isMagicLinkEnabled) {
                showMagicLinkRequestScreen(email, verifyEmail, allowPassword = true, forceRequestAtStart = false)
            } else {
                showEmailPasswordScreen(email, verifyEmail)
            }
        }
    }

    private fun showEmailPasswordScreen(
        email: String?,
        verifyEmail: Boolean,
        password: String? = null
    ) {
        val wooLoginEmailPasswordFragment = WooLoginEmailPasswordFragment
            .newInstance(
                emailAddress = email,
                password = password,
                verifyMagicLinkEmail = verifyEmail
            )
        changeFragment(wooLoginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    private fun showMagicLinkRequestScreen(
        email: String?,
        verifyEmail: Boolean,
        allowPassword: Boolean,
        forceRequestAtStart: Boolean
    ) {
        val scheme = WOOCOMMERCE
        val loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment
            .newInstance(
                email,
                scheme,
                false,
                null,
                verifyEmail,
                allowPassword,
                forceRequestAtStart
            )
        changeFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG, false)
    }

    override fun loginViaSiteAddress() {
        unifiedLoginTracker.setFlowAndStep(LOGIN_SITE_ADDRESS, ENTER_SITE_ADDRESS)
        val loginSiteAddressFragment = getLoginViaSiteAddressFragment() ?: WooLoginSiteAddressFragment()
        changeFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG)
    }

    private fun showPrologueCarouselFragment() {
        val fragment = LoginPrologueCarouselFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, LoginPrologueCarouselFragment.TAG)
            .addToBackStack(LoginPrologueCarouselFragment.TAG)
            .commitAllowingStateLoss()
    }

    private fun showPrologueFragment() = lifecycleScope.launch {
        withStarted { // suspend until the fragment is started
            val prologueFragment = getPrologueFragment() ?: LoginPrologueFragment()
            changeFragment(prologueFragment, true, LoginPrologueFragment.TAG)
        }
    }

    override fun loginViaSocialAccount(
        email: String?,
        idToken: String?,
        service: String?,
        isPasswordRequired: Boolean
    ) {
        val loginEmailPasswordFragment = WooLoginEmailPasswordFragment.newInstance(
            emailAddress = email,
            idToken = idToken,
            service = service,
            isSocialLogin = isPasswordRequired
        )
        changeFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun loggedInViaSocialAccount(oldSitesIds: ArrayList<Int>, doLoginUpdate: Boolean) {
        loginAnalyticsListener.trackLoginSocialSuccess()
        showMainActivityAndFinish()
    }

    override fun loginViaWpcomUsernameInstead() {
        val loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
            "wordpress.com",
            "wordpress.com",
            null,
            null,
            true
        )
        changeFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG)
    }

    override fun showMagicLinkSentScreen(email: String?, allowPassword: Boolean) {
        val loginMagicLinkSentFragment = LoginMagicLinkSentImprovedFragment.newInstance(email, true)
        changeFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentImprovedFragment.TAG, false)
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
        val loginEmailPasswordFragment = WooLoginEmailPasswordFragment.newInstance(email)
        changeFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG)
    }

    override fun forgotPassword(url: String) {
        loginAnalyticsListener.trackLoginForgotPasswordClicked()
        ChromeCustomTabUtils.launchUrl(this, url.slashJoin(FORGOT_PASSWORD_URL_SUFFIX))
    }

    override fun needs2fa(email: String?, password: String?) {
        loginAnalyticsListener.trackLogin2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstance(email, password)
        changeFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2fa(
        email: String?,
        password: String?,
        userId: String?,
        webauthnNonce: String?,
        nonceAuthenticator: String?,
        nonceBackup: String?,
        noncePush: String?,
        supportedAuthTypes: MutableList<String>?
    ) {
        loginAnalyticsListener.trackLogin2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstance(
            email,
            password,
            userId,
            webauthnNonce,
            nonceAuthenticator,
            nonceBackup,
            noncePush,
            supportedAuthTypes
        )
        changeFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocial(
        email: String?,
        userId: String?,
        nonceAuthenticator: String?,
        nonceBackup: String?,
        nonceSms: String?,
        nonceWebauthn: String?,
        supportedAuthTypes: MutableList<String>?
    ) {
        loginAnalyticsListener.trackLoginSocial2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstanceSocial(
            email,
            userId,
            nonceAuthenticator,
            nonceBackup,
            nonceSms,
            nonceWebauthn,
            supportedAuthTypes
        )
        changeFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun needs2faSocialConnect(email: String?, password: String?, idToken: String?, service: String?) {
        loginAnalyticsListener.trackLoginSocial2faNeeded()
        val login2FaFragment = Login2FaFragment.newInstanceSocialConnect(email, password, idToken, service)
        changeFragment(login2FaFragment, true, Login2FaFragment.TAG)
    }

    override fun loggedInViaPassword(oldSitesIds: ArrayList<Int>) {
        showMainActivityAndFinish()
    }

    override fun alreadyLoggedInWpcom(oldSitesIds: ArrayList<Int>) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG)
        showMainActivityAndFinish()
    }

    override fun gotWpcomSiteInfo(siteAddress: String?) {
        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app.
        siteAddress?.let { appPrefsWrapper.setLoginSiteAddress(it) }
        showEmailLoginScreen(siteAddress)
    }

    override fun gotConnectedSiteInfo(siteAddress: String, redirectUrl: String?, hasJetpack: Boolean) {
        // If the redirect url is available, use that as the preferred url. Pass this url to the other fragments
        // with the protocol since it is needed for initiating forgot password flow etc in the login process.
        val inputSiteAddress = urlUtils.sanitiseUrl(redirectUrl ?: siteAddress)

        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app. Strip the protocol from this url string prior to saving to AppPrefs since it's
        // not needed and may cause issues when attempting to match the url to the authenticated account later
        // in the login process.
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = inputSiteAddress.replaceFirst(protocolRegex, "")
        appPrefsWrapper.setLoginSiteAddress(siteAddressClean)
        if (hasJetpack || connectSiteInfo?.isWPCom == true) {
            showEmailLoginScreen(null)
        } else {
            loginViaSiteCredentials(inputSiteAddress)
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
        // hide the keyboard
        org.wordpress.android.util.ActivityUtils.hideKeyboard(this)

        unifiedLoginTracker.trackClick(Click.LOGIN_WITH_SITE_CREDS)

        clearCachedSites()
        showUsernamePasswordScreen(inputSiteAddress, null, null, null)
    }

    override fun gotXmlRpcEndpoint(inputSiteAddress: String?, endpointAddress: String?) {
        // Save site address to app prefs so it's available to MainActivity regardless of how the user
        // logs into the app.
        inputSiteAddress?.let { appPrefsWrapper.setLoginSiteAddress(it) }

        showUsernamePasswordScreen(inputSiteAddress, endpointAddress, null, null)
    }

    override fun handleSslCertificateError(
        memorizingTrustManager: MemorizingTrustManager?,
        callback: LoginListener.SelfSignedSSLCallback?
    ) {
        WooLog.e(WooLog.T.LOGIN, "Self-signed SSL certificate detected - can't proceed with the login.")
        // TODO: Support self-signed SSL sites and show dialog (only needed when XML-RPC support is added)
    }

    private fun viewHelpAndSupport(origin: HelpOrigin, extraTags: ArrayList<String>? = null) {
        val flow = unifiedLoginTracker.getFlow()
        val step = unifiedLoginTracker.previousStepBeforeHelpStep

        startActivity(HelpActivity.createIntent(this, origin, extraTags, flow?.value, step?.value))
    }

    override fun helpSiteAddress(url: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_SITE_ADDRESS)
    }

    override fun helpFindingSiteAddress(username: String?, siteStore: SiteStore?) {
        unifiedLoginTracker.trackClick(Click.HELP_FINDING_SITE_ADDRESS)
        startActivity(
            SupportRequestFormActivity.createIntent(
                context = this,
                origin = HelpOrigin.LOGIN_SITE_ADDRESS,
                extraTags = ArrayList()
            )
        )
    }

    // TODO This can be modified to also receive the URL the user entered, so we can make that the primary store
    override fun loggedInViaUsernamePassword(oldSitesIds: ArrayList<Int>) {
        showMainActivityAndFinish()
    }

    override fun helpEmailScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_EMAIL)
    }

    override fun helpSocialEmailScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_SOCIAL)
    }

    @Suppress("DEPRECATION")
    override fun addGoogleLoginFragment(isSignupFromLoginEnabled: Boolean) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val loginGoogleFragment = LoginGoogleFragment().apply {
            retainInstance = true
        }
        fragmentTransaction.add(loginGoogleFragment, LoginGoogleFragment.TAG)
        fragmentTransaction.commitAllowingStateLoss()
    }

    override fun helpMagicLinkRequest(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_MAGIC_LINK)
    }

    override fun helpMagicLinkSent(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_MAGIC_LINK)
    }

    override fun helpEmailPasswordScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_EMAIL_PASSWORD)
    }

    override fun help2FaScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.LOGIN_2FA)
    }

    override fun startPostLoginServices() {
        // TODO Start future NotificationsUpdateService
    }

    override fun helpUsernamePassword(url: String?, username: String?, isWpcom: Boolean) {
        val extraSupportTags = if (!isWpcom) {
            arrayListOf(APPLICATION_PASSWORD_LOGIN_ZENDESK_TAG)
        } else {
            null
        }
        viewHelpAndSupport(HelpOrigin.LOGIN_USERNAME_PASSWORD, extraTags = extraSupportTags)
    }

    override fun helpNoJetpackScreen(
        siteAddress: String,
        endpointAddress: String?,
        username: String,
        password: String,
        userAvatarUrl: String?,
        checkJetpackAvailability: Boolean
    ) {
        if (connectSiteInfo?.isJetpackActive == true) {
            // If jetpack is present, but we can't find the connected email, then show account mismatch error
            val fragment = AccountMismatchErrorFragment().apply {
                arguments = AccountMismatchErrorFragmentArgs(
                    siteUrl = siteAddress,
                    primaryButton = AccountMismatchPrimaryButton.CONNECT_JETPACK,
                    errorType = AccountMismatchErrorType.ACCOUNT_NOT_CONNECTED
                ).toBundle()
            }
            changeFragment(
                fragment = fragment,
                shouldAddToBackStack = true,
                tag = AccountMismatchErrorFragment::class.java.simpleName
            )
        } else {
            val jetpackReqFragment = LoginNoJetpackFragment.newInstance(
                siteAddress,
                endpointAddress,
                username,
                password,
                userAvatarUrl,
                checkJetpackAvailability
            )
            changeFragment(
                fragment = jetpackReqFragment as Fragment,
                shouldAddToBackStack = true,
                tag = LoginNoJetpackFragment.TAG
            )
        }
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
            siteAddress,
            endpointAddress,
            username,
            password,
            userAvatarUrl,
            errorMessage
        )
        changeFragment(
            fragment = discoveryErrorFragment as Fragment,
            shouldAddToBackStack = true,
            tag = LoginDiscoveryErrorFragment.TAG
        )
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

    override fun helpSignupEmailScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.SIGNUP_EMAIL)
    }

    override fun helpSignupMagicLinkScreen(email: String?) {
        viewHelpAndSupport(HelpOrigin.SIGNUP_MAGIC_LINK)
    }

    override fun showSignupMagicLink(email: String?) {
        // TODO: Signup
    }

    override fun showSignupToLoginMessage() {
        // TODO: Signup
    }

    override fun onTermsOfServiceClicked() {
        ChromeCustomTabUtils.launchUrl(this, urlUtils.tosUrlWithLocale, ThreeQuarters)
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
        msg?.let {
            uiMessageResolver.showSnack(it)
        }
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
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_BY_EMAIL_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED)
        unifiedLoginTracker.trackClick(Click.HELP_FINDING_CONNECTED_EMAIL)

        LoginEmailHelpDialogFragment.newInstance(this)
            .show(supportFragmentManager, LoginEmailHelpDialogFragment.TAG)
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(HelpActivity.createIntent(this, HelpOrigin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }

    override fun showEmailLoginScreen(siteAddress: String?) {
        if (siteAddress != null) {
            // Show the layout that includes the option to login with site credentials.
            val loginEmailFragment = getLoginEmailFragment(
                siteCredsLayout = true
            ) ?: LoginEmailFragment.newInstance(siteAddress, true)
            changeFragment(loginEmailFragment as Fragment, true, LoginEmailFragment.TAG_SITE_CREDS_LAYOUT)
        } else {
            val loginEmailFragment = getLoginEmailFragment(
                siteCredsLayout = false
            ) ?: WooLoginEmailFragment.newInstance(showSiteCredentialsFallback = connectSiteInfo?.isWPCom == false)
            changeFragment(loginEmailFragment as Fragment, true, LoginEmailFragment.TAG)
        }
    }

    override fun showUsernamePasswordScreen(
        siteAddress: String?,
        endpointAddress: String?,
        inputUsername: String?,
        inputPassword: String?
    ) = changeFragment(
        fragment = LoginSiteCredentialsFragment.newInstance(
            siteAddress = requireNotNull(siteAddress),
            isJetpackConnected = connectSiteInfo?.isJetpackConnected ?: false,
            username = inputUsername,
            password = inputPassword
        ),
        shouldAddToBackStack = true,
        tag = LoginSiteCredentialsFragment.TAG
    )

    override fun onApplicationPasswordHelpRequired(url: String, errorMessage: String) {
        changeFragment(
            fragment = ApplicationPasswordTutorialFragment.newInstance(url, errorMessage),
            shouldAddToBackStack = true,
            tag = ApplicationPasswordTutorialFragment.TAG
        )
    }

    override fun startJetpackInstall(siteAddress: String?) {
        siteAddress?.let {
            val url = "$JETPACK_CONNECT_URL?url=$it&mobile_redirect=$JETPACK_CONNECTED_REDIRECT_URL&from=mobile"
            ChromeCustomTabUtils.launchUrl(this, url)
        }
    }

    override fun gotUnregisteredEmail(email: String?) {
        // Show the 'No WordPress.com account found' screen
        LoginNoWPcomAccountFoundDialogFragment().show(LoginNoWPcomAccountFoundDialogFragment.TAG)
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

    override fun useMagicLinkInstead(email: String?, verifyEmail: Boolean) {
        showMagicLinkRequestScreen(email, verifyEmail, allowPassword = false, forceRequestAtStart = true)
    }

    /**
     * Allows for special handling of errors that come up during the login by address: check site address.
     */
    override fun handleSiteAddressError(siteInfo: ConnectSiteInfoPayload) {
        if (!siteInfo.isWordPress) {
            // hide the keyboard
            org.wordpress.android.util.ActivityUtils.hideKeyboard(this)

            // show the "not WordPress error" screen
            LoginNotWPDialogFragment().show(LoginNotWPDialogFragment.TAG)
        } else {
            // Just in case we use this method for a different scenario in the future
            TODO("Handle a new error scenario")
        }
    }

    override fun onWhatIsWordPressLinkClicked() {
        ChromeCustomTabUtils.launchUrl(this, LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT)
        unifiedLoginTracker.trackClick(Click.WHAT_IS_WORDPRESS_COM)
    }

    override fun onLoginWithSiteCredentialsFallbackClicked() {
        loginViaSiteCredentials(appPrefsWrapper.getLoginSiteAddress())
    }

    override fun onCarouselFinished() {
        showPrologueFragment()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onFetchedConnectSiteInfo(event: OnConnectSiteInfoChecked) {
        connectSiteInfo = if (event.isError) {
            null
        } else {
            event.info.let {
                ConnectSiteInfo(
                    isWPCom = it.isWPCom,
                    isJetpackConnected = it.isJetpackConnected,
                    isJetpackActive = it.isJetpackActive
                )
            }
        }
    }

    override fun onScanQrCodeClicked(source: String) {
        AnalyticsTracker.track(
            stat = AnalyticsEvent.LOGIN_WITH_QR_CODE_BUTTON_TAPPED,
            properties = mapOf(
                KEY_FLOW to VALUE_LOGIN_WITH_WORDPRESS_COM,
                KEY_SOURCE to source
            )
        )
        openQrCodeScannerFragment()
    }

    private fun clearCachedSites() {
        // Clear all sites from the DB to avoid any conflicts with the new login
        // Sometimes, the same website could be fetched from different APIs (WPCom or WPApi), and if cached twice
        // during successive login attempts, it leads to some issues later
        dispatcher.dispatch(SiteActionBuilder.newRemoveAllSitesAction())
    }

    private fun handleAppLoginUri(uri: Uri) {
        unifiedLoginTracker.setFlow(Flow.LOGIN_QR.value)
        val siteUrl = uri.getQueryParameter(SITE_URL_PARAMETER) ?: ""
        val wpComEmail = uri.getQueryParameter(WP_COM_EMAIL_PARAMETER) ?: ""
        val username = uri.getQueryParameter(USERNAME_PARAMETER) ?: ""
        when {
            siteUrl.isNotEmpty() && wpComEmail.isNotEmpty() -> {
                gotWpcomSiteInfo(siteUrl)
                AnalyticsTracker.track(
                    stat = AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
                    properties = mapOf(KEY_FLOW to VALUE_WP_COM)
                )
                showEmailPasswordScreen(email = wpComEmail, verifyEmail = false, password = null)
            }

            siteUrl.isNotEmpty() && username.isNotEmpty() -> {
                AnalyticsTracker.track(
                    stat = AnalyticsEvent.LOGIN_APP_LOGIN_LINK_SUCCESS,
                    properties = mapOf(KEY_FLOW to VALUE_NO_WP_COM)
                )
                showUsernamePasswordScreen(
                    siteAddress = siteUrl,
                    inputUsername = username,
                    endpointAddress = null,
                    inputPassword = null
                )
            }

            else -> {
                AnalyticsTracker.track(
                    stat = AnalyticsEvent.LOGIN_MALFORMED_APP_LOGIN_LINK,
                    properties = mapOf(KEY_URL to uri.toString())
                )
                ToastUtils.showToast(this, R.string.login_app_login_malformed_link)
                showPrologue()
            }
        }
    }

    private fun openQrCodeScannerFragment() {
        GmsBarcodeScanning.getClient(this).startScan()
            .addOnSuccessListener { rawValue ->
                if (ValidateScannedValue.validate(rawValue.rawValue)) {
                    AnalyticsTracker.track(stat = AnalyticsEvent.LOGIN_WITH_QR_CODE_SCANNED)
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawValue.rawValue))
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        resources.getText(R.string.not_a_valid_qr_code),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    /**
     * Show a DialogFragment using the current Fragment's childFragmentManager.
     * This is useful to make sure the dialog's lifecycle is linked to the Fragment that invokes it and that it would
     * be dismissed when we navigate to another Fragment.
     */
    private fun DialogFragment.show(tag: String) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)!!

        show(currentFragment.childFragmentManager, tag)
    }

    @Parcelize
    private data class ConnectSiteInfo(
        val isWPCom: Boolean,
        val isJetpackConnected: Boolean,
        val isJetpackActive: Boolean
    ) : Parcelable
}
