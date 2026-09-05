package com.woocommerce.android.ui.ageeligibility

import android.content.Context
import com.google.android.play.agesignals.AgeSignalsManager
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AgeSignalsModule {
    @Binds
    @Singleton
    abstract fun bindAgeSignalsClient(impl: GoogleAgeSignalsClient): AgeSignalsClient

    companion object {
        @Provides
        @Singleton
        fun provideAgeSignalsManager(@ApplicationContext context: Context): AgeSignalsManager =
            AgeSignalsManagerFactory.create(context)
    }
}
