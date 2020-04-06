package com.woocommerce.android.ui.products

import android.content.Context
import com.woocommerce.android.ui.products.WcProductTestUtils.generateProductSettings
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.store.WooCommerceStore

class MockedWooStore constructor(
    appContext: Context,
    dispatcher: Dispatcher,
    wcCoreRestClient: WooCommerceRestClient
) : WooCommerceStore(
        appContext,
        dispatcher,
        wcCoreRestClient
) {
    var settings: WCProductSettingsModel? = null

    override fun getProductSettings(site: SiteModel): WCProductSettingsModel? {
        return settings ?: generateProductSettings()
    }
}
