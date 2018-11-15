package com.woocommerce.android

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.support.multidex.MultiDexApplication
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.WooCommerceGlideModule
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.woocommerce.android.util.CrashlyticsUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.yarolegovich.wellsql.WellSql
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.utils.ErrorUtils.OnUnexpectedError
import org.wordpress.android.util.PackageUtils
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasActivityInjector, HasServiceInjector, ApplicationLifecycleListener {
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    @Inject lateinit var membersInjector: MembersInjector<WooCommerceGlideModule>

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var accountStore: AccountStore

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var zendeskHelper: ZendeskHelper

    // Listens for changes in device connectivity
    @Inject lateinit var connectionReceiver: ConnectionChangeReceiver
    private var connectionReceiverRegistered = false

    protected open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        val wellSqlConfig = WellSqlConfig(applicationContext, WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(wellSqlConfig)

        component.inject(this)
        dispatcher.register(this)

        AppPrefs.init(this)

        initAnalytics()

        val site = if (selectedSite.exists()) {
            selectedSite.get()
        } else {
            null
        }
        CrashlyticsUtils.initCrashlytics(this, accountStore.account, site)

        createNotificationChannelsOnSdk26()

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        registerActivityLifecycleCallbacks(lifecycleMonitor)
        registerComponentCallbacks(lifecycleMonitor)

        trackStartupAnalytics()

        zendeskHelper.setupZendesk(
                this, BuildConfig.ZENDESK_DOMAIN, BuildConfig.ZENDESK_APP_ID,
                BuildConfig.ZENDESK_OAUTH_CLIENT_ID
        )
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

        val oldVersionCode = AppPrefs.getLastAppVersionCode()

        if (oldVersionCode == versionCode) {
            return
        }

        if (oldVersionCode == 0) {
            // Track application installed if there isn't old version code
            AnalyticsTracker.track(Stat.APPLICATION_INSTALLED)
        } else if (oldVersionCode < versionCode) {
            AnalyticsTracker.track(Stat.APPLICATION_UPGRADED)
        }
        AppPrefs.setLastAppVersionCode(versionCode)
    }

    private fun createNotificationChannelsOnSdk26() {
        // Create Notification channels introduced in Android Oreo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create the General channel
            val generalChannel = NotificationChannel(
                    getString(R.string.notification_channel_general_id),
                    getString(R.string.notification_channel_general_title),
                    NotificationManager.IMPORTANCE_DEFAULT)
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!accountStore.hasAccessToken()) {
            // Logged out
            AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT)

            // Reset analytics
            AnalyticsTracker.flush()
            AnalyticsTracker.clearAllData()
            CrashlyticsUtils.resetAccountAndSite()
            zendeskHelper.reset()

            // Wipe user-specific preferences
            AppPrefs.reset()
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // make sure local usage tracking matches the account setting
            val hasUserOptedOut = !AnalyticsTracker.sendUsageStats
            if (hasUserOptedOut != accountStore.account.tracksOptOut) {
                AnalyticsTracker.sendUsageStats = !accountStore.account.tracksOptOut
            }
            CrashlyticsUtils.initAccount(accountStore.account)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUnexpectedError(event: OnUnexpectedError) {
        with(event) {
            CrashlyticsUtils.logException(exception, message = "FluxC: ${exception.message}: $description")
        }
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector
}
