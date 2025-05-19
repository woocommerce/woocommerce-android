@file:OptIn(ExperimentalCoroutinesApi::class)

package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundledProduct
import org.wordpress.android.fluxc.model.WCProductComponent
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.dao.ProductsDao

@Suppress("LargeClass")
internal object ProductSqlUtils {
    private val gson by lazy { Gson() }

    suspend fun ProductsDao.getCompositeProducts(site: SiteModel, remoteProductId: Long): List<WCProductComponent> {
        val productModel = getProduct(site.id, remoteProductId)

        return productModel?.let {
            val responseType = object : TypeToken<List<WCProductComponent>>() {}.type
            gson.fromJson(it.compositeComponents, responseType) as? List<WCProductComponent>
        } ?: emptyList()
    }

    private fun getBundledProducts(
        productModel: WCProductModel?,
    ): List<WCBundledProduct> {
        return productModel?.let {
            val responseType = object : TypeToken<List<WCBundledProduct>>() {}.type
            gson.fromJson(it.bundledItems, responseType) as? List<WCBundledProduct>
        } ?: emptyList()
    }

    fun ProductsDao.observeBundledProducts(
        site: SiteModel,
        remoteProductId: Long
    ): Flow<List<WCBundledProduct>> {
        return observeProducts(
            localSiteId = site.id,
            remoteProductId = remoteProductId
        ).mapLatest { product ->
            getBundledProducts(product.firstOrNull())
        }
    }
}
