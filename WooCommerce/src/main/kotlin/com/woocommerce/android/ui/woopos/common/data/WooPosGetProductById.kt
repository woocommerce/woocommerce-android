package com.woocommerce.android.ui.woopos.common.data

import android.util.Log
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Inject

class WooPosGetProductById @Inject constructor(
    private val selectedSite: SelectedSite,
    private val cache: WooPosProductsCache,
    private val productRestClient: ProductRestClient,
) {
    suspend operator fun invoke(productId: Long): Product? = withContext(IO) {
        Log.d(TAG, "getProductById: productId=$productId")
        
        // First check cache
        val cachedProduct = cache.getProductById(productId)
        if (cachedProduct != null) {
            Log.d(TAG, "getProductById: Found product in cache: ${cachedProduct.name}")
            return@withContext cachedProduct
        }

        Log.d(TAG, "getProductById: Product not in cache, fetching from remote")
        // Product not in cache, fetch from remote
        try {
            val remoteProductResult = productRestClient.fetchSingleProduct(
                site = selectedSite.get(),
                remoteProductId = productId,
            )

            return@withContext if (!remoteProductResult.isError) {
                val remoteProduct = remoteProductResult.productWithMetaData.product
                val product = remoteProduct.toAppModel()
                
                // Add to cache if there's space, otherwise replace oldest
                if (cache.getAll().size < WooPosProductsCache.MAX_CACHE_SIZE) {
                    cache.addAll(listOf(product))
                } else {
                    Log.d(TAG, "getProductById: Cache full, replacing with new product")
                    cache.clear()
                    cache.addAll(listOf(product))
                }
                
                Log.d(TAG, "getProductById: Successfully fetched product: ${product.name}")
                product
            } else {
                Log.e(TAG, "getProductById: Failed to fetch product $productId: ${remoteProductResult.error}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProductById: Exception fetching product $productId", e)
            null
        }
    }
    
    companion object {
        private const val TAG = "WooPosGetProductById"
    }
}
