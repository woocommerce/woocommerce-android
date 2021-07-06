package com.woocommerce.android.ui.login

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import org.wordpress.android.login.di.LoginFragmentModule

@Module(includes = [LoginFragmentModule::class])
@InstallIn(ActivityComponent::class)
internal abstract class WooLoginFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [MagicLinkInterceptModule::class])
    internal abstract fun magicLinkInterceptFragment(): MagicLinkInterceptFragment
}
