package com.woocommerce.android.ui.woopos.common.di

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
interface WooPosPINModule {
    @Binds
    fun bindPINStorage(storage: WooPosEncryptedPINStorage): WooPosPINStorage

    @Binds
    fun bindPINCredentialSource(store: WooPosLocalPINStore): WooPosPINCredentialSource
}
