package com.woocommerce.android.ui.orders.filters.domain

import com.woocommerce.android.extensions.isSitePublic
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.list.OrderListRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ShouldShowCreateTestOrderScreen @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderListRepository: OrderListRepository,
    private val productListRepository: ProductListRepository
) {
    suspend operator fun invoke(): Boolean {
        val site = selectedSite.get()

        return site.isSitePublic &&
            orderListRepository.getAllPaymentGateways(site).isNotEmpty() &&
            checkPublishedProductsExist()
    }

    private suspend fun checkPublishedProductsExist(): Boolean {
        val localProductList = productListRepository.getProductList()

        val productList = localProductList.ifEmpty {
            withContext(Dispatchers.IO) {
                productListRepository.fetchProductList().getOrNull() ?: productListRepository.getProductList()
            }
        }

        return productList.any { it.status == ProductStatus.PUBLISH }
    }
}
