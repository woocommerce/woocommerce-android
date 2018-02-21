package com.woocommerce.android.di

import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainModule
import com.woocommerce.android.ui.order.OrderDetailModule
import com.woocommerce.android.ui.orderlist.OrderListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class ActivityBindingModule {
    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(
            MainModule::class,
            OrderListModule::class,
            OrderDetailModule::class))
    abstract fun provideMainActivityInjector(): MainActivity

    @ActivityScoped
    @ContributesAndroidInjector(modules = arrayOf(LoginFragmentModule::class))
    abstract fun provideLoginActivityInjector(): LoginActivity
}
