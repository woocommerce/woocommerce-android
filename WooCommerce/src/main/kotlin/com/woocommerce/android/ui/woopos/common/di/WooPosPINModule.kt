package com.woocommerce.android.ui.woopos.common.di

import com.woocommerce.android.ui.woopos.lock.WooPosAndroidPINClock
import com.woocommerce.android.ui.woopos.lock.WooPosFileLockedPOSStore
import com.woocommerce.android.ui.woopos.lock.WooPosFilePINAttemptStorage
import com.woocommerce.android.ui.woopos.lock.WooPosLockedPOSStore
import com.woocommerce.android.ui.woopos.lock.WooPosPINAttemptStorage
import com.woocommerce.android.ui.woopos.lock.WooPosPINClock
import com.woocommerce.android.ui.woopos.lock.WooPosPINRecovery
import com.woocommerce.android.ui.woopos.lock.WooPosPendingPINRecovery
import com.woocommerce.android.ui.woopos.roles.WooPosEncryptedPINStorage
import com.woocommerce.android.ui.woopos.roles.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.WooPosPINCredentialSource
import com.woocommerce.android.ui.woopos.roles.WooPosPINStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface WooPosPINModule {
    @Binds
    fun bindLockedPOSStore(store: WooPosFileLockedPOSStore): WooPosLockedPOSStore

    @Binds
    fun bindPINClock(clock: WooPosAndroidPINClock): WooPosPINClock

    @Binds
    fun bindAttemptStorage(storage: WooPosFilePINAttemptStorage): WooPosPINAttemptStorage

    @Binds
    fun bindRecovery(recovery: WooPosPendingPINRecovery): WooPosPINRecovery

    @Binds
    fun bindPINStorage(storage: WooPosEncryptedPINStorage): WooPosPINStorage

    @Binds
    fun bindPINCredentialSource(store: WooPosLocalPINStore): WooPosPINCredentialSource
}
