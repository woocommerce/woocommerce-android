package com.woocommerce.android.ui.login

import com.woocommerce.android.di.FragmentScope
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class LoginNoJetpackFragmentModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = [LoginNoJetpackModule::class])
    internal abstract fun loginNoJetpackFragment(): LoginNoJetpackFragment
}
