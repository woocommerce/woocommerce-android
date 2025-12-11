package com.woocommerce.android.ui.ageeligibility

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgeSignalsModule {
    @Binds
    @Singleton
    abstract fun bindAgeSignalsClient(impl: DefaultAgeSignalsClient): AgeSignalsClient
}
