package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.ui.products.ProductStatus
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.DownloadableOptions
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class WooPosProductsTypesFilter @Inject constructor() {
    val filters: Map<ProductFilterOption, String> =
        mapOf(
            ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
            ProductFilterOption.DOWNLOADABLE to DownloadableOptions.FALSE.toString()
        )

    val includeTypes: List<WCProductStore.IncludeType> =
        listOf(
            WCProductStore.IncludeType.Simple,
            WCProductStore.IncludeType.Variable
        )
}
