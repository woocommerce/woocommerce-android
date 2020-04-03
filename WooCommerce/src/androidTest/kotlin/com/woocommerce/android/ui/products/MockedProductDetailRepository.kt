package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.WcProductTestUtils.generateProductDetail
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCTaxStore
import javax.inject.Inject

class MockedProductDetailRepository constructor(
    dispatcher: Dispatcher,
    productStore: WCProductStore,
    selectedSite: SelectedSite,
    taxStore: WCTaxStore
) : ProductDetailRepository(
        dispatcher,
        productStore,
        selectedSite,
        taxStore
) {
    var product: Product? = null

    override suspend fun fetchProduct(remoteProductId: Long): Product? {
        return getProduct(remoteProductId)
    }

    override fun getProduct(remoteProductId: Long): Product? {
        return product ?: generateProductDetail().toAppModel()
    }

    override suspend fun fetchProductShippingClassById(remoteShippingClassId: Long): ShippingClass? {
        return getProductShippingClassByRemoteId(remoteShippingClassId)
    }

    override fun getProductShippingClassByRemoteId(remoteShippingClassId: Long): ShippingClass? {
        return ShippingClass()
    }

    override fun getTaxClassesForSite(): List<TaxClass> {
        return emptyList()
    }
}
