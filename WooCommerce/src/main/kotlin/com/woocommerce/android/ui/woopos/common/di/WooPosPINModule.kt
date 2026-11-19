package com.woocommerce.android.ui.woopos.common.di

import com.woocommerce.android.ui.woopos.roles.lock.WooPosAndroidPINClock
import com.woocommerce.android.ui.woopos.roles.lock.WooPosFilePINAttemptStorage
import com.woocommerce.android.ui.woopos.roles.lock.WooPosPINAttemptStorage
import com.woocommerce.android.ui.woopos.roles.lock.WooPosPINClock
import com.woocommerce.android.ui.woopos.roles.lock.WooPosPINRecovery
import com.woocommerce.android.ui.woopos.roles.lock.WooPosPendingPINRecovery
import com.woocommerce.android.ui.woopos.roles.pin.WooPosEncryptedPINStorage
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WooPosPINModule {
    @Binds
    fun bindPINClock(clock: WooPosAndroidPINClock): WooPosPINClock

    @Binds
    fun bindAttemptStorage(storage: WooPosFilePINAttemptStorage): WooPosPINAttemptStorage

    @Binds
    fun bindRecovery(recovery: WooPosPendingPINRecovery): WooPosPINRecovery

    @Binds
    fun bindPINStorage(storage: WooPosEncryptedPINStorage): WooPosPINStorage
}
