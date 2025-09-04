package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import javax.inject.Inject

class WooPosGetVariationById @Inject constructor(
    private val posStore: WooPosLocalCatalogStore,
    private val site: SelectedSite,
) {
    suspend operator fun invoke(productId: Long, variationId: Long): WooPosVariation? = withContext(IO) {
        val siteModel = site.getOrNull() ?: return@withContext null
        val result = posStore.getVariation(
            siteId = LocalId(siteModel.id),
            productId = RemoteId(productId),
            variationId = RemoteId(variationId)
        )

        result.getOrNull()?.toWooPosVariation()
    }
}
