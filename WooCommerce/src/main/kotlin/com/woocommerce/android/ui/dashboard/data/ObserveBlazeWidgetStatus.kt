package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.withIndex
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class ObserveBlazeWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val productListRepository: ProductListRepository
) {
    operator fun invoke() = selectedSite.observe()
        .filterNotNull()
        .map { isBlazeEnabled() }
        .combine(observePublishedProductsCount()) { isBlazeEnabled, hasPublishedProducts ->
            if (isBlazeEnabled && hasPublishedProducts) {
                DashboardWidget.Status.Available
            } else {
                DashboardWidget.Status.Hidden
            }
        }

    private fun observePublishedProductsCount() = productListRepository
        .observeProductsCount(
            filterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value),
            excludeSampleProducts = true
        )
        .withIndex()
        .map { (index, productsCount) ->
            if (productsCount == 0L && index == 0) {
                productListRepository
                    .fetchProductList(
                        productFilterOptions = mapOf(ProductFilterOption.STATUS to ProductStatus.PUBLISH.value)
                    ).getOrNull()
                    ?.filterNot { it.isSampleProduct }
                    ?.size != 0
            } else {
                productsCount > 0
            }
        }
        .distinctUntilChanged()
}
