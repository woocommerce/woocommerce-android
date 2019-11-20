package com.woocommerce.android.ui.login

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
internal abstract class LoginNoJetpackModule {
    @ContributesAndroidInjector
    internal abstract fun loginNoJetpackFragment(): LoginNoJetpackFragment
}
