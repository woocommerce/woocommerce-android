package com.woocommerce.android

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import com.woocommerce.android.di.WooCommerceGlideModule
import com.yarolegovich.wellsql.WellSql
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import dagger.android.HasServiceInjector
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import javax.inject.Inject

open class WooCommerce : Application(), HasActivityInjector, HasServiceInjector {
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
        WellSql.init(WellSqlConfig(applicationContext))

        val lifecycleMonitor = ApplicationLifecycleMonitor()
        registerComponentCallbacks(lifecycleMonitor)
        registerActivityLifecycleCallbacks(lifecycleMonitor)

        AnalyticsTracker.init(applicationContext)
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector

    override fun serviceInjector(): AndroidInjector<Service> = serviceInjector

    private class ApplicationLifecycleMonitor : Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
        override fun onActivityPaused(activity: Activity?) {
        }

        override fun onActivityStarted(activity: Activity?) {
        }

        override fun onActivityDestroyed(activity: Activity?) {
        }

        override fun onActivitySaveInstanceState(activity: Activity?, bundle: Bundle?) {
        }

        override fun onActivityStopped(activity: Activity?) {
        }

        override fun onActivityCreated(activity: Activity?, bundle: Bundle?) {
        }

        override fun onActivityResumed(activity: Activity?) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_OPENED)
        }

        override fun onConfigurationChanged(config: Configuration?) {
        }

        override fun onLowMemory() {
        }

        override fun onTrimMemory(level: Int) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_CLOSED)
            AnalyticsTracker.flush()
        }
    }
}
