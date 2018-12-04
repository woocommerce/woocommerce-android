package com.woocommerce.android.push

import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class FCMServiceModule {
    @ContributesAndroidInjector
    internal abstract fun fcmRegistrationIntentService(): FCMRegistrationIntentService

    @ContributesAndroidInjector
    internal abstract fun fcmMessageService(): FCMMessageService
}
