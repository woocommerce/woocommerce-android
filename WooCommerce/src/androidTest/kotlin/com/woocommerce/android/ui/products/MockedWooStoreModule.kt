package com.woocommerce.android.ui.products

import android.content.Context
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.store.WooCommerceStore

@Module
object MockedWooStoreModule {
    var settings: WCProductSettingsModel? = null

    @JvmStatic
    @Provides
    fun provideWooStore(
        appContext: Context,
        dispatcher: Dispatcher,
        wcCoreRestClient: WooCommerceRestClient
    ): MockedWooStore {
        val store = MockedWooStore(appContext, dispatcher, wcCoreRestClient)
        store.settings = settings
        return store
    }
}
