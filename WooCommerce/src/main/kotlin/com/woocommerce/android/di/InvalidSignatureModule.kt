package com.woocommerce.android.di

import com.woocommerce.android.network.StoreConnectionErrorMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpcom.wc.InvalidSignatureListener

// The optional binding is declared in commons' InvalidSignatureListenerModule (installed in every Hilt graph);
// the main app additionally binds the concrete implementation here.
@Module
@InstallIn(SingletonComponent::class)
interface InvalidSignatureModule {
    @Binds
    fun bindInvalidSignatureListener(
        monitor: StoreConnectionErrorMonitor
    ): InvalidSignatureListener
}
