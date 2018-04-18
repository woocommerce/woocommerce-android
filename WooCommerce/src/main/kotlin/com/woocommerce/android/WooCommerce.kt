package com.woocommerce.android

import android.app.Activity
import android.app.Service
import android.support.multidex.MultiDexApplication
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.WooCommerceGlideModule
import com.woocommerce.android.util.ApplicationLifecycleMonitor
import com.woocommerce.android.util.ApplicationLifecycleMonitor.ApplicationLifecycleListener
import com.yarolegovich.wellsql.WellSql
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasActivityInjector, HasServiceInjector, ApplicationLifecycleListener {
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>
    @Inject lateinit var serviceInjector: DispatchingAndroidInjector<Service>

    @Inject lateinit var membersInjector: MembersInjector<WooCommerceGlideModule>

    protected open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()

        component.inject(this)
        val wellSqlConfig = WellSqlConfig(applicationContext, WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(wellSqlConfig)

        AnalyticsTracker.init(applicationContext)

        val lifecycleMonitor = ApplicationLifecycleMonitor(this)
        registerActivityLifecycleCallbacks(lifecycleMonitor)
        registerComponentCallbacks(lifecycleMonitor)
    }

    override fun onAppComesFromBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_OPENED)
    }

    override fun onAppGoesToBackground() {
        AnalyticsTracker.track(Stat.APPLICATION_CLOSED)
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector
}
