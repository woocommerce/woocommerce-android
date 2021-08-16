package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.push.WooNotificationBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NotificationModule {
    @Provides
    @Singleton
    fun provideWooNotificationBuilder(context: Context) = WooNotificationBuilder(context)
}
