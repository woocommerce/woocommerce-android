package com.woocommerce.android.ui.bookings.tab

import com.woocommerce.android.ciab.isCIABSite
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class ShowBookingsTab @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productListRepository: ProductListRepository
) {

    operator fun invoke(): Flow<Boolean> {
        return combine(
            selectedSite.observe(),
            productListRepository
                .observeProductsCount(
                    filterOptions = mapOf(
                        ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
                        ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE
                    ),
                    excludeSampleProducts = true
                ),
        ) { site, productsCount ->
            productsCount > 0 &&
                site?.isCIABSite() == true &&
                FeatureFlag.BOOKINGS_MVP.isEnabled()
        }.onStart {
            productListRepository.fetchProductList(
                productFilterOptions = mapOf(ProductFilterOption.TYPE to BOOKING_PRODUCT_TYPE)
            ).onFailure {
                WooLog.w(WooLog.T.BOOKINGS, "Failed to fetch bookable products" )
            }
        }.distinctUntilChanged()
    }

    companion object {
        private const val BOOKING_PRODUCT_TYPE = "booking"
    }
}
