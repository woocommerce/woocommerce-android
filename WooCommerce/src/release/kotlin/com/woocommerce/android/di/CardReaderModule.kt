package com.woocommerce.android.di

import com.woocommerce.android.cardreader.CardReaderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.annotation.Nullable
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CardReaderModule {
    @Provides
    @Nullable
    @Singleton
    fun provideCardReaderManager(): CardReaderManager? = null
}
