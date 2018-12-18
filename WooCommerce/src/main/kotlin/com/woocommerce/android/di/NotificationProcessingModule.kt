package com.woocommerce.android.di

import com.woocommerce.android.push.NotificationsProcessingService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class NotificationProcessingModule {
    @ContributesAndroidInjector
    internal abstract fun notificationsProceesingService(): NotificationsProcessingService
}
