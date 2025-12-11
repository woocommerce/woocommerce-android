package com.woocommerce.android.ui.login

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class AgeSignalsModule {
    @Binds
    abstract fun bindAgeSignalsClient(impl: DefaultAgeSignalsClient): AgeSignalsClient
}
