package com.woocommerce.android.ui.login

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class LoginPrologueModule {
    @ContributesAndroidInjector
    abstract fun loginPrologueFragment(): LoginPrologueFragment
}
