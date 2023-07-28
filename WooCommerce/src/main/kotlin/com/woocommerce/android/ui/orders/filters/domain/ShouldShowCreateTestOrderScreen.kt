package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class ShouldShowCreateTestOrderScreen @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderListRepository: OrderListRepository,
    private val productListRepository: ProductListRepository
) {
    operator fun invoke(): Boolean {
        val site = selectedSite.get()
        return FeatureFlag.CREATE_TEST_ORDER.isEnabled() &&
            site.isSitePublic &&
            orderListRepository.getAllPaymentGateways(site).isNotEmpty() &&
            productListRepository.getProductList().any {
                it.status == ProductStatus.PUBLISH
            }
    }
}
