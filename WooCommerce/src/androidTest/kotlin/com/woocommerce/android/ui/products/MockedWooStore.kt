package com.woocommerce.android.ui.products

import android.content.Context
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.WcProductTestUtils.generateProductDetail
import com.woocommerce.android.ui.products.WcProductTestUtils.generateProductSettings
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooCommerceRestClient
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCTaxStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

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
