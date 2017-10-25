package com.woocommerce.android

import android.app.Activity
import android.app.Application
import com.woocommerce.android.di.AppComponent
import com.woocommerce.android.di.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject

open class WooCommerce : Application(), HasActivityInjector {
    @Inject lateinit var activityInjector: DispatchingAndroidInjector<Activity>

    protected open val component: AppComponent by lazy {
        DaggerAppComponent.builder()
                .application(this)
                .build()
    }

    override fun onCreate() {
        super.onCreate()
        component.inject(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> = activityInjector
}
