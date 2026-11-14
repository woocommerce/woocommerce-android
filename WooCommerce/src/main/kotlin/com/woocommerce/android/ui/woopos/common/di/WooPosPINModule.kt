package com.woocommerce.android.ui.woopos.common.di

import com.woocommerce.android.ui.woopos.roles.pin.WooPosEncryptedPINStorage
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface WooPosPINModule {
    @Binds
    fun bindPINStorage(storage: WooPosEncryptedPINStorage): WooPosPINStorage
}
