package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosGetVariationById @Inject constructor(
    private val store: WCProductStore,
    private val site: SelectedSite,
) {
    suspend operator fun invoke(productId: Long, variationId: Long): WooPosVariation? = withContext(IO) {
        val siteModel = site.getOrNull() ?: return@withContext null
        store.getVariationByRemoteId(siteModel, productId, variationId)?.toAppModel()?.toWooPosVariation()
    }
}
