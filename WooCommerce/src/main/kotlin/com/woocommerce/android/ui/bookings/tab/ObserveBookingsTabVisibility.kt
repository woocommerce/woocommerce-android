package com.woocommerce.android.ui.bookings.tab

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.extensions.isCIABSite
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class ObserveBookingsTabVisibility @Inject constructor(
    private val productListRepository: ProductListRepository
) {

    operator fun invoke(siteModel: SiteModel): Flow<Boolean> = productListRepository.observeProductsCount(
        filterOptions = mapOf(
            ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
            ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE
        ),
        excludeSampleProducts = true
    ).onStart {
        productListRepository.fetchProductList(
            productFilterOptions = mapOf(ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE)
        ).onFailure {
            WooLog.w(WooLog.T.BOOKINGS, "Failed to fetch bookable products")
        }
    }
        .map { productsCount ->
            productsCount > 0 &&
                siteModel?.isCIABSite() == true &&
                FeatureFlag.BOOKINGS_MVP.isEnabled()
        }
        .distinctUntilChanged()

    companion object {
        @VisibleForTesting
        const val BOOKING_PRODUCT_TYPE = "booking"
    }
}
