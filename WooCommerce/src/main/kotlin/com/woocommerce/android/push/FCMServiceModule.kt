package com.woocommerce.android.push

import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
abstract class FCMServiceModule {
    @ContributesAndroidInjector
    internal abstract fun fcmRegistrationIntentService(): FCMRegistrationIntentService

    @ContributesAndroidInjector
    internal abstract fun fcmMessageService(): FCMMessageService
}
