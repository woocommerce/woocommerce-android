package com.woocommerce.android

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDexApplication
import com.android.volley.VolleyLog
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.WooCommerceGlideModule
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.RateLimitedTask
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.woocommerce.android.util.CrashUtils
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.PackageUtils.PACKAGE_VERSION_CODE_DEFAULT
import com.woocommerce.android.util.REGEX_API_JETPACK_TUNNEL_METHOD
import com.woocommerce.android.util.REGEX_API_NUMERIC_PARAM
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.widgets.AppRatingDialog
import com.yarolegovich.wellsql.WellSql
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.generated.WCCoreActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.OnJetpackTimeoutError
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.WCPayStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasAndroidInjector, ApplicationLifecycleListener {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var membersInjector: MembersInjector<WooCommerceGlideModule>

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore // Required to ensure the SiteStore is initialized
    @Inject lateinit var wooCommerceStore: WooCommerceStore // Required to ensure the WooCommerceStore is initialized

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var zendeskHelper: ZendeskHelper
    @Inject lateinit var notificationHandler: NotificationHandler

    // Listens for changes in device connectivity
    @Inject lateinit var connectionReceiver: ConnectionChangeReceiver

    @Inject lateinit var prefs: AppPrefs

    @Inject lateinit var payStore: WCPayStore

    // TODO cardreader init this field
    open val cardReaderManager: CardReaderManager? = null

    private var connectionReceiverRegistered = false

    open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    companion object {
        private const val SECONDS_BETWEEN_SITE_UPDATE = 60 * 60 // 1 hour
    }

    /**
     * Update WP.com and WooCommerce settings in a background task.
     */
    private val updateSelectedSite: RateLimitedTask = object : RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        override fun run(): Boolean {
            selectedSite.getIfExists()?.let {
                dispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(it))
                dispatcher.dispatch(WCCoreActionBuilder.newFetchSiteSettingsAction(it))
                dispatcher.dispatch(WCCoreActionBuilder.newFetchProductSettingsAction(it))
            }
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()

        // enable strict mode in debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        val wellSqlConfig = WooWellSqlConfig(applicationContext)
        WellSql.init(wellSqlConfig)

        CrashUtils.initCrashLogging(this)

        component.inject(this)

        FeedbackPrefs.init(this)

        // Apply Theme
        AppThemeUtils.setAppTheme()

        dispatcher.register(this)

        AppRatingDialog.init(this)

        initAnalytics()

        val site = if (selectedSite.exists()) {
            selectedSite.get()
        } else {
            null
        }

        // Developers can uncomment the line below to clear the db tables at startup
        // wellSqlConfig.resetDatabase()

        notificationHandler.createNotificationChannels(this)

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        registerActivityLifecycleCallbacks(lifecycleMonitor)
        registerComponentCallbacks(lifecycleMonitor)

        trackStartupAnalytics()

        zendeskHelper.setupZendesk(
                this, BuildConfig.ZENDESK_DOMAIN, BuildConfig.ZENDESK_APP_ID,
                BuildConfig.ZENDESK_OAUTH_CLIENT_ID
        )
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private fun enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            WooLog.e(T.UTILS, "You should not call enableStrictMode() on a non debug build")
            return
        }

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build()
        )
        WooLog.w(T.UTILS, "Strict mode enabled")
    }

    override fun onAppComesFromBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_OPENED)

        if (!connectionReceiverRegistered) {
            connectionReceiverRegistered = true
            registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }

        if (isGooglePlayServicesAvailable(applicationContext)) {
            // Register for Cloud messaging
            FCMRegistrationIntentService.enqueueWork(this)
        }

        if (networkStatus.isConnected()) {
            updateSelectedSite.runIfNotLimited()
        }
    }

    override fun onFirstActivityResumed() {
        // Update the WP.com account details, settings, and site list every time the app is completely restarted
        if (networkStatus.isConnected()) {
            dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(FetchSitesPayload()))
        }
    }

    override fun onAppGoesToBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_CLOSED)

        if (connectionReceiverRegistered) {
            connectionReceiverRegistered = false
            unregisterReceiver(connectionReceiver)
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val connectionResult = googleApiAvailability.isGooglePlayServicesAvailable(context)

        return when (connectionResult) {
            ConnectionResult.SUCCESS -> true
            else -> {
                WooLog.w(T.NOTIFS, "Google Play Services unavailable, connection result: " +
                        googleApiAvailability.getErrorString(connectionResult))
                return false
            }
        }
    }

    private fun initAnalytics() {
        AnalyticsTracker.init(applicationContext)

        if (selectedSite.exists()) {
            AnalyticsTracker.refreshMetadata(accountStore.account?.userName, selectedSite.get())
        } else {
            AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
        }
    }

    private fun trackStartupAnalytics() {
        // Track app upgrade and install
        val versionCode = PackageUtils.getVersionCode(this)
        val oldVersionCode = prefs.getLastAppVersionCode()

        if (oldVersionCode == 0) {
            AnalyticsTracker.track(Stat.APPLICATION_INSTALLED)

            // Store the current app version code to SharedPrefs, even if the value is -1
            // to prevent duplicate install events being called
            prefs.setLastAppVersionCode(versionCode)
        } else if (oldVersionCode < versionCode) {
            // Track upgrade event only if oldVersionCode is not -1, to prevent
            // duplicate upgrade events being called
            if (oldVersionCode > PACKAGE_VERSION_CODE_DEFAULT) {
                AnalyticsTracker.track(Stat.APPLICATION_UPGRADED)
            }

            // store the latest version code to SharedPrefs, only if the value
            // is greater than the stored version code
            prefs.setLastAppVersionCode(versionCode)
        } else if (versionCode == PACKAGE_VERSION_CODE_DEFAULT) {
            // we are not able to read the current app version code
            // track this event along with the last stored version code
            AnalyticsTracker.track(
                    Stat.APPLICATION_VERSION_CHECK_FAILED,
                    mapOf(AnalyticsTracker.KEY_LAST_KNOWN_VERSION_CODE to oldVersionCode)
            )
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        val isLoggedOut = event.causeOfChange == null && event.error == null
        if (!accountStore.hasAccessToken() && isLoggedOut) {
            // Logged out
            AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT)

            // Reset analytics
            AnalyticsTracker.flush()
            AnalyticsTracker.clearAllData()
            CrashUtils.resetAccountAndSite()
            zendeskHelper.reset()

            // Wipe user-specific preferences
            prefs.resetUserPreferences()
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // make sure local usage tracking matches the account setting
            val hasUserOptedOut = !AnalyticsTracker.sendUsageStats
            if (hasUserOptedOut != accountStore.account.tracksOptOut) {
                AnalyticsTracker.sendUsageStats = !accountStore.account.tracksOptOut
            }
            CrashUtils.setCurrentAccount(accountStore.account)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnexpectedError(event: OnUnexpectedError) {
        with(event) {
            CrashUtils.logException(exception, message = "FluxC: ${exception.message}: $description")
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
            AnalyticsTracker.track(Stat.JETPACK_TUNNEL_TIMEOUT, properties)
        }
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
