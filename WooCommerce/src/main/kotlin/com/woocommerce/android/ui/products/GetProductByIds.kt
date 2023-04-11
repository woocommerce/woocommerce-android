package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductByIds @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val dispatchers: CoroutineDispatchers
) {
    operator fun invoke(productIds: List<Long>): Flow<GetProductsStatus> {
        return merge( getLocalData(productIds), refreshProductData(productIds))
            .flowOn(dispatchers.io)
            .onStart { GetProductsStatus.Loading }
    }

    private fun getLocalData(productIds: List<Long>): Flow<GetProductsStatus> {
        return productStore.observeProducts(selectedSite.get(), productIds)
            .mapLatest { list ->
                val products = list.map { product -> product.toAppModel() }
                GetProductsStatus.Succeeded(products)
            }
    }

    private fun refreshProductData(productIds: List<Long>): Flow<GetProductsStatus> = flow {
        productStore.fetchProductListSynced(selectedSite.get(), productIds)
            ?.map { product -> product.toAppModel() }
            ?.let { GetProductsStatus.Succeeded(it) }
            ?: GetProductsStatus.Failed(Exception("Failed to fetch products from the API"))
    }

    sealed interface GetProductsStatus {
        object Loading : GetProductsStatus
        data class Succeeded(val products: List<Product>) : GetProductsStatus
        data class Failed(val throwable: Throwable) : GetProductsStatus
    }
}
