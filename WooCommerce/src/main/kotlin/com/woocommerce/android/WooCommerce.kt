package com.woocommerce.android

import android.app.Activity
import android.app.Service
import android.support.multidex.MultiDexApplication
import com.crashlytics.android.Crashlytics
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.WooCommerceGlideModule
import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.AppLog as WordPressAppLog
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.woocommerce.android.util.CrashlyticsUtils
import com.yarolegovich.wellsql.WellSql
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import io.fabric.sdk.android.Fabric
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.util.PackageUtils
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasActivityInjector, HasServiceInjector, ApplicationLifecycleListener {
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    @Inject lateinit var membersInjector: MembersInjector<WooCommerceGlideModule>

    @Inject lateinit var dispatcher: Dispatcher
    @Inject lateinit var accountStore: AccountStore

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

        if (!PackageUtils.isDebugBuild()) {
            Fabric.with(this, Crashlytics())

            // Send logs for app events through to Crashlytics
            WooLog.addListener { tag, logLevel, message ->
                CrashlyticsUtils.log("$logLevel/${WooLog.TAG}-$tag: $message")
            }

            // Send logs for library events (FluxC, Login, utils) through to Crashlytics
            WordPressAppLog.addListener { tag, logLevel, message ->
                CrashlyticsUtils.log("$logLevel/${WordPressAppLog.TAG}-$tag: $message")
            }
        }

        initAnalytics()

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        registerActivityLifecycleCallbacks(lifecycleMonitor)
        registerComponentCallbacks(lifecycleMonitor)

        trackStartupAnalytics()
    }

    override fun onAppComesFromBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_OPENED)
    }

    override fun onAppGoesToBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_CLOSED)
    }

    private fun initAnalytics() {
        AnalyticsTracker.init(applicationContext)
        AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!accountStore.hasAccessToken()) {
            // Logged out
            AnalyticsTracker.track(Stat.ACCOUNT_LOGOUT)

            // Reset analytics
            AnalyticsTracker.flush()
            AnalyticsTracker.clearAllData()

            // Wipe user-specific preferences
            AppPrefs.reset()
        }
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector
}
