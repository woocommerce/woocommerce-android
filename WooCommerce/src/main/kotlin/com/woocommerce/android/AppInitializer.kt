package com.woocommerce.android

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.ProcessLifecycleOwner
import com.automattic.android.experimentation.ExPlat
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.config.WPComRemoteFeatureFlagRepository
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.lesserThan
import com.woocommerce.android.extensions.pastTimeDeltaFromNowInDays
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.push.FCMRefreshWorker
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.support.zendesk.ZendeskSettings
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.RateLimitedTask
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.SiteConnectionType.ApplicationPasswords
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.tracker.SendTelemetry
import com.woocommerce.android.tracker.TrackStoreSnapshot
import com.woocommerce.android.ui.appwidgets.getWidgetName
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.jitm.JitmStoreInMemoryCache
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
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
import dagger.Lazy
import dagger.android.DispatchingAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
class AppInitializer @Inject constructor() : ApplicationLifecycleListener {
    companion object {
        private const val SECONDS_BETWEEN_SITE_UPDATE = 60 * 60 // 1 hour
        private const val UNAUTHORIZED_STATUS_CODE = 401
        private const val CARD_READER_USAGE_THIRTY_DAYS = 30
    }

    @Inject lateinit var crashLogging: CrashLogging

    @Inject lateinit var fluxCCrashLogger: FluxCCrashLogger

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var dispatcher: Dispatcher

    @Inject lateinit var accountStore: AccountStore

    @Inject lateinit var accountRepository: Lazy<AccountRepository>

    @Inject lateinit var siteStore: SiteStore // Required to ensure the SiteStore is initialized

    @Inject lateinit var wooCommerceStore: WooCommerceStore // Required to ensure the WooCommerceStore is initialized

    @Inject lateinit var selectedSite: SelectedSite

    @Inject lateinit var networkStatus: NetworkStatus

    @Inject lateinit var zendeskSettings: ZendeskSettings

    @Inject lateinit var userEligibilityFetcher: UserEligibilityFetcher

    @Inject lateinit var uploadEncryptedLogs: UploadEncryptedLogs

    @Inject lateinit var observeEncryptedLogsUploadResults: ObserveEncryptedLogsUploadResult

    @Inject lateinit var sendTelemetry: SendTelemetry

    @Inject lateinit var siteObserver: SiteObserver

    @Inject lateinit var wooLog: WooLogWrapper

    @Inject lateinit var registerDevice: RegisterDevice

    @Inject lateinit var applicationPasswordsNotifier: ApplicationPasswordsNotifier

    @Inject lateinit var featureFlagRepository: WPComRemoteFeatureFlagRepository

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject lateinit var explat: ExPlat

    // Listens for changes in device connectivity
    @Inject lateinit var connectionReceiver: ConnectionChangeReceiver

    @Inject lateinit var prefs: AppPrefs

    @Inject lateinit var getWooVersion: GetWooCorePluginCachedVersion

    @Inject @AppCoroutineScope
    lateinit var appCoroutineScope: CoroutineScope

    @Inject lateinit var cardReaderOnboardingChecker: CardReaderOnboardingChecker

    @Inject lateinit var jitmStoreInMemoryCache: JitmStoreInMemoryCache

    @Inject lateinit var trackStoreSnapshot: TrackStoreSnapshot

    @Inject lateinit var notificationChannelsHandler: NotificationChannelsHandler

    private var connectionReceiverRegistered = false

    private lateinit var application: Application

    /**
     * Update WP.com and WooCommerce settings in a background task.
     */
    private val updateSelectedSite: RateLimitedTask = object : RateLimitedTask(SECONDS_BETWEEN_SITE_UPDATE) {
        override fun run(): Boolean {
            selectedSite.getIfExists()?.let {
                appCoroutineScope.launch {
                    wooCommerceStore.fetchWooCommerceSite(it).let {
                        if (it.model?.hasWooCommerce == false && it.model?.connectionType == ApplicationPasswords) {
                            // The previously selected site doesn't have Woo anymore, take the user to the login screen
                            WooLog.w(T.LOGIN, "Selected site no longer has WooCommerce")

                            selectedSite.reset()
                            restartMainActivity()
                        }
                    }
                    wooCommerceStore.fetchSiteGeneralSettings(it)
                    wooCommerceStore.fetchSiteProductSettings(it)
                }
            }
            return true
        }
    }

    fun init(application: Application) {
        this.application = application

        crashLogging.initialize()
        Thread.setDefaultUncaughtExceptionHandler(
            UncaughtErrorsHandler(
                context = application,
                baseHandler = Thread.getDefaultUncaughtExceptionHandler(),
                crashLogger = crashLogging
            )
        )

        // Apply Theme
        AppThemeUtils.setAppTheme()

        dispatcher.register(this)

        FluxCCrashLoggerProvider.initLogger(fluxCCrashLogger)

        AppRatingDialog.init(application)

        initAnalytics()

        notificationChannelsHandler.init()

        // Developers can uncomment the line below to clear the db tables at startup
        // wellSqlConfig.resetDatabase()

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        application.registerActivityLifecycleCallbacks(lifecycleMonitor)
        application.registerComponentCallbacks(lifecycleMonitor)

        trackStartupAnalytics()

        zendeskSettings.setup(
            context = application,
            zendeskUrl = BuildConfig.ZENDESK_DOMAIN,
            applicationId = BuildConfig.ZENDESK_APP_ID,
            oauthClientId = BuildConfig.ZENDESK_OAUTH_CLIENT_ID
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
        appCoroutineScope.launch {
            featureFlagRepository.fetchFeatureFlags(PackageUtils.getVersionName(application.applicationContext))
        }

        monitorApplicationPasswordsStatus()

        // Schedule worker to refresh FCM token periodically
        FCMRefreshWorker.schedule(application)
    }

    @Suppress("DEPRECATION")
    override fun onAppComesFromBackground() {
        trackApplicationOpened()

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
        // App is completely restarted
        if (networkStatus.isConnected()) {
            if (accountStore.hasAccessToken()) {
                // Update the WPCom account if the user is signed in using a WPCom account
                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            }

            // Update the list of sites
            appCoroutineScope.launch {
                wooCommerceStore.fetchWooCommerceSites()

                // Added to fix this crash
                // https://github.com/woocommerce/woocommerce-android/issues/4842
                if (selectedSite.getSelectedSiteId() != -1 &&
                    !selectedSite.exists()
                ) {
                    // The previously selected site is not connected anymore, take the user to the site picker
                    WooLog.i(DASHBOARD, "Selected site no longer exists, showing site picker")
                    restartMainActivity()
                }
            }

            if (selectedSite.exists()) {
                appCoroutineScope.launch {
                    userEligibilityFetcher.fetchUserInfo().onSuccess {
                        if (!it.isEligible) {
                            WooLog.w(T.LOGIN, "Current user is not eligible to access the current site")
                            restartMainActivity()
                        }
                    }

                    buildList {
                        val isIPPUser = Date(
                            prefs.getCardReaderLastSuccessfulPaymentTime()
                        ).pastTimeDeltaFromNowInDays lesserThan CARD_READER_USAGE_THIRTY_DAYS

                        if (isIPPUser) {
                            add(
                                async {
                                    cardReaderOnboardingChecker.invalidateCache()
                                    cardReaderOnboardingChecker.getOnboardingState()
                                }
                            )
                        }

                        add(async { jitmStoreInMemoryCache.init() })
                        add(async { trackStoreSnapshot() })
                    }.awaitAll()
                }
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

    private fun restartMainActivity() {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(STARTED)) {
            val intent = Intent(application, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            application.startActivity(intent)
        }
    }

    private fun monitorApplicationPasswordsStatus() {
        suspend fun logUserOut() {
            accountRepository.get().logout()
            restartMainActivity()
        }

        appCoroutineScope.launch {
            // Log user out if the Application Passwords feature gets disabled
            applicationPasswordsNotifier.featureUnavailableEvents
                .onEach {
                    if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
                        WooLog.w(T.LOGIN, "Application Passwords support has been disabled in the current site")
                        logUserOut()
                    }
                }.launchIn(this)

            // Log user out if the Application Passwords generation fails due to a 401 error
            applicationPasswordsNotifier.passwordGenerationFailures
                .filter { it.networkError.volleyError?.networkResponse?.statusCode == UNAUTHORIZED_STATUS_CODE }
                .onEach {
                    if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
                        WooLog.w(T.LOGIN, "Use is unauthorized to generate a new application password")
                        logUserOut()
                    }
                }.launchIn(this)
        }
    }

    private fun initAnalytics() {
        AnalyticsTracker.init(
            application,
            selectedSite,
            prefs,
            getWooVersion,
            appCoroutineScope,
        )

        AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
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
        val isLoggedOut = event.causeOfChange == AccountAction.SIGN_OUT && event.error == null
        if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            analyticsTracker.sendUsageStats = !accountStore.account.tracksOptOut
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
            val genericPath = apiPath?.replace(REGEX_API_NUMERIC_PARAM, "/ID/")
            val protocol = REGEX_API_JETPACK_TUNNEL_METHOD.find(apiPath ?: "")?.groups?.get(1)?.value ?: ""

            val properties = mapOf(
                "path" to genericPath,
                "protocol" to protocol,
                "times_retried" to timesRetried.toString()
            )
            AnalyticsTracker.track(AnalyticsEvent.JETPACK_TUNNEL_TIMEOUT, properties)
        }
    }

    private fun trackApplicationOpened() {
        val widgetManager = AppWidgetManager.getInstance(application)

        val widgets = widgetManager.installedProviders.filter { providerInfo ->
            // We only care about WooCommerce widgets so we filter providerInfo by packageName
            // and we also check that it has at least one widget id (at least one widget installed)
            providerInfo.provider.packageName == application.packageName &&
                widgetManager.getAppWidgetIds(providerInfo.provider).isNotEmpty()
        }.map { providerInfo -> providerInfo.getWidgetName() }

        AnalyticsTracker.track(
            stat = AnalyticsEvent.APPLICATION_OPENED,
            properties = mapOf(AnalyticsTracker.KEY_WIDGETS to widgets)
        )
    }
}
