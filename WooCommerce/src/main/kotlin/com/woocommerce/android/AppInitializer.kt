package com.woocommerce.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.ProcessLifecycleOwner
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.push.RegisterDevice
import com.woocommerce.android.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.push.WooNotificationBuilder
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.RateLimitedTask
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tracker.SendTelemetry
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.REGEX_API_JETPACK_TUNNEL_METHOD
import com.woocommerce.android.util.REGEX_API_NUMERIC_PARAM
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.DASHBOARD
import com.woocommerce.android.util.WooLog.T.UTILS
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.util.crashlogging.UploadEncryptedLogs
import com.woocommerce.android.util.encryptedlogging.ObserveEncryptedLogsUploadResult
import com.woocommerce.android.widgets.AppRatingDialog
import dagger.android.DispatchingAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.logging.FluxCCrashLoggerProvider
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.OnJetpackTimeoutError
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInitializer @Inject constructor() : ApplicationLifecycleListener {
    companion object {
        private const val SECONDS_BETWEEN_SITE_UPDATE = 60 * 60 // 1 hour
    }

    @Inject lateinit var crashLogging: CrashLogging
    @Inject lateinit var fluxCCrashLogger: FluxCCrashLogger
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore // Required to ensure the SiteStore is initialized
    @Inject lateinit var wooCommerceStore: WooCommerceStore // Required to ensure the WooCommerceStore is initialized

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var zendeskHelper: ZendeskHelper
    @Inject lateinit var wooNotificationBuilder: WooNotificationBuilder
    @Inject lateinit var userEligibilityFetcher: UserEligibilityFetcher
    @Inject lateinit var uploadEncryptedLogs: UploadEncryptedLogs
    @Inject lateinit var observeEncryptedLogsUploadResults: ObserveEncryptedLogsUploadResult
    @Inject lateinit var sendTelemetry: SendTelemetry
    @Inject lateinit var siteObserver: SiteObserver
    @Inject lateinit var wooLog: WooLogWrapper
    @Inject lateinit var registerDevice: RegisterDevice

    // Listens for changes in device connectivity
    @Inject lateinit var connectionReceiver: ConnectionChangeReceiver

    @Inject lateinit var prefs: AppPrefs

    @Inject @AppCoroutineScope lateinit var appCoroutineScope: CoroutineScope

    private var connectionReceiverRegistered = false

    private lateinit var application: Application

    /**
     * Update WP.com and WooCommerce settings in a background task.
     */
    private val updateSelectedSite: RateLimitedTask = object : RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        override fun run(): Boolean {
            selectedSite.getIfExists()?.let {
                appCoroutineScope.launch {
                    wooCommerceStore.fetchWooCommerceSite(it)
                    wooCommerceStore.fetchSiteGeneralSettings(it)
                    wooCommerceStore.fetchSiteProductSettings(it)
                }
            }
            return true
        }
    }

    fun init(application: Application) {
        this.application = application

        FeedbackPrefs.init(application)

        // Apply Theme
        AppThemeUtils.setAppTheme()

        dispatcher.register(this)

        FluxCCrashLoggerProvider.initLogger(fluxCCrashLogger)

        AppRatingDialog.init(application)

        initAnalytics()

        // Developers can uncomment the line below to clear the db tables at startup
        // wellSqlConfig.resetDatabase()

        wooNotificationBuilder.createNotificationChannels()

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        application.registerActivityLifecycleCallbacks(lifecycleMonitor)
        application.registerComponentCallbacks(lifecycleMonitor)

        trackStartupAnalytics()

        zendeskHelper.setupZendesk(
            application, BuildConfig.ZENDESK_DOMAIN, BuildConfig.ZENDESK_APP_ID,
            BuildConfig.ZENDESK_OAUTH_CLIENT_ID
        )

        observeEncryptedLogsUploadResults()
        uploadEncryptedLogs()

        appCoroutineScope.launch {
            sendTelemetry(BuildConfig.VERSION_NAME).collect { result ->
                wooLog.i(UTILS, "WCTracker telemetry result: $result")
            }
        }
        appCoroutineScope.launch {
            siteObserver.observeAndUpdateSelectedSiteData()
        }
    }

    override fun onAppComesFromBackground() {
        AnalyticsTracker.track(AnalyticsEvent.APPLICATION_OPENED)

        if (!connectionReceiverRegistered) {
            connectionReceiverRegistered = true
            application.registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

        if (networkStatus.isConnected()) {
            updateSelectedSite.runIfNotLimited()

            appCoroutineScope.launch {
                registerDevice(IF_NEEDED)
            }
        }
    }

    override fun onFirstActivityResumed() {
        // Update the WP.com account details, settings, and site list every time the app is completely restarted,
        // only if the logged in
        if (networkStatus.isConnected() && accountStore.hasAccessToken()) {
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            appCoroutineScope.launch {
                wooCommerceStore.fetchWooCommerceSites()

                // Added to fix this crash
                // https://github.com/woocommerce/woocommerce-android/issues/4842
                if (selectedSite.getSelectedSiteId() != -1 &&
                    !selectedSite.exists() &&
                    ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(STARTED)
                ) {
                    // The previously selected site is not connected anymore, take the user to the site picker
                    WooLog.i(DASHBOARD, "Selected site no longer exists, showing site picker")
                    val intent = Intent(application, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    application.startActivity(intent)
                }
            }

            // Update the user info for the currently logged in user
            if (selectedSite.exists()) {
                userEligibilityFetcher.fetchUserEligibility()
            }
        }
    }

    override fun onAppGoesToBackground() {
        AnalyticsTracker.track(AnalyticsEvent.APPLICATION_CLOSED)

        if (connectionReceiverRegistered) {
            connectionReceiverRegistered = false
            application.unregisterReceiver(connectionReceiver)
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()

        return when (val connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(context)) {
            ConnectionResult.SUCCESS -> true
            else -> {
                WooLog.w(
                    T.NOTIFS,
                    "Google Play Services unavailable, connection result: " +
                        googleApiAvailability.getErrorString(connectionResult)
                )
                return false
            }
        }
    }

    private fun initAnalytics() {
        AnalyticsTracker.init(application)

        if (selectedSite.exists()) {
            AnalyticsTracker.refreshMetadata(accountStore.account?.userName, selectedSite.get())
        } else {
            AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
        }
    }

    private fun trackStartupAnalytics() {
        // Track app upgrade and install
        val versionCode = PackageUtils.getVersionCode(application)
        val oldVersionCode = prefs.getLastAppVersionCode()

        when {
            oldVersionCode == 0 -> {
                AnalyticsTracker.track(AnalyticsEvent.APPLICATION_INSTALLED)
                // Store the current app version code to SharedPrefs, even if the value is -1
                // to prevent duplicate install events being called
                prefs.setLastAppVersionCode(versionCode)
            }
            oldVersionCode < versionCode -> {
                // Track upgrade event only if oldVersionCode is not -1, to prevent
                // duplicate upgrade events being called
                if (oldVersionCode > PackageUtils.PACKAGE_VERSION_CODE_DEFAULT) {
                    AnalyticsTracker.track(AnalyticsEvent.APPLICATION_UPGRADED)
                }

                // store the latest version code to SharedPrefs, only if the value
                // is greater than the stored version code
                prefs.setLastAppVersionCode(versionCode)
            }
            versionCode == PackageUtils.PACKAGE_VERSION_CODE_DEFAULT -> {
                // we are not able to read the current app version code
                // track this event along with the last stored version code
                AnalyticsTracker.track(
                    AnalyticsEvent.APPLICATION_VERSION_CHECK_FAILED,
                    mapOf(AnalyticsTracker.KEY_LAST_KNOWN_VERSION_CODE to oldVersionCode)
                )
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        val isLoggedOut = event.causeOfChange == null && event.error == null
        if (!accountStore.hasAccessToken() && isLoggedOut) {
            // Logged out
            AnalyticsTracker.track(AnalyticsEvent.ACCOUNT_LOGOUT)

            // Reset analytics
            AnalyticsTracker.flush()
            AnalyticsTracker.clearAllData()
            zendeskHelper.reset()

            // Wipe user-specific preferences
            prefs.resetUserPreferences()
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // make sure local usage tracking matches the account setting
            val hasUserOptedOut = !AnalyticsTracker.sendUsageStats
            if (hasUserOptedOut != accountStore.account.tracksOptOut) {
                AnalyticsTracker.sendUsageStats = !accountStore.account.tracksOptOut
            }
        }

        val userAccountFetched = !isLoggedOut && event.causeOfChange == AccountAction.FETCH_ACCOUNT
        if (userAccountFetched) {
            appCoroutineScope.launch {
                registerDevice(IF_NEEDED)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnexpectedError(event: OnUnexpectedError) {
        with(event) {
            crashLogging.sendReport(exception = exception, message = "FluxC: ${exception.message}: $description")
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onJetpackTimeoutError(event: OnJetpackTimeoutError) {
        with(event) {
            // Replace numeric IDs with a placeholder so events can be aggregated
            val genericPath = apiPath.replace(REGEX_API_NUMERIC_PARAM, "/ID/")
            val protocol = REGEX_API_JETPACK_TUNNEL_METHOD.find(apiPath)?.groups?.get(1)?.value ?: ""

            val properties = mapOf(
                "path" to genericPath,
                "protocol" to protocol,
                "times_retried" to timesRetried.toString()
            )
            AnalyticsTracker.track(AnalyticsEvent.JETPACK_TUNNEL_TIMEOUT, properties)
        }
    }
}
