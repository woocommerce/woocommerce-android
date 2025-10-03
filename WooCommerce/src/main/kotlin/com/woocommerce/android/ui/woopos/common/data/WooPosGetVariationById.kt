package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosGetVariationById @Inject constructor(
    private val store: WCProductStore,
    private val site: SelectedSite,
    private val mapper: WooPosVariationMapper,
    private val localCatalogStore: WooPosLocalCatalogStore,
    private val wooPosLocalCatalogM1Enabled: WooPosLocalCatalogM1Enabled,
) {
    suspend operator fun invoke(productId: Long, variationId: Long): WooPosVariation? = withContext(IO) {
        when {
            wooPosLocalCatalogM1Enabled() -> {
                val siteModel = site.getOrNull() ?: return@withContext null
                val result = localCatalogStore.getVariation(
                    siteId = siteModel.id,
                    productId = productId,
                    variationId = variationId
                )
                result.getOrNull()?.toWooPosVariation(mapper)
            }
            else -> getFromInMemoryCache(productId, variationId)
        }
    }

    private suspend fun getFromInMemoryCache(productId: Long, variationId: Long): WooPosVariation? {
        val siteModel = site.getOrNull() ?: return null
        return store.getVariationByRemoteId(siteModel, productId, variationId)?.toWooPosVariation(mapper)
    }
}
