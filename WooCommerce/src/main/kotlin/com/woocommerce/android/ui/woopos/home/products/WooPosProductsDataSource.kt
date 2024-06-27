package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.store.WCProductStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductsDataSource @Inject constructor(private val handler: ProductListHandler) {
    var hasMorePages: AtomicBoolean = AtomicBoolean(false)

    val products: Flow<List<Product>> = handler.productsFlow
        .onEach { hasMorePages.set(it.size % PAGE_SIZE == 0) }
        .map { it.filter { product -> product.price != null } }

    suspend fun loadSimpleProducts() {
        handler.loadFromCacheAndFetch(
            searchType = ProductListHandler.SearchType.DEFAULT,
            filters = mapOf(WCProductStore.ProductFilterOption.TYPE to ProductType.SIMPLE.value)
        )
    }

    suspend fun loadMore() {
        handler.loadMore()
    }

    private companion object {
        // the product list handler doesn't expose page size
        // so workaround to copy it here
        const val PAGE_SIZE = 25
    }
}
