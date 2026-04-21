package com.woocommerce.android.ui.bookings.tab

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.onFirst
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.bookings.BookingsRepository
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.list.ProductListRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsOrderOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

class ObserveBookingsVisibility @Inject constructor(
    private val productListRepository: ProductListRepository,
    private val bookingsRepository: BookingsRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Boolean> =
        selectedSite.observe()
            .filterNotNull()
            .flatMapLatest { siteModel ->
                observeBookingsVisibilityForSite(siteModel)
            }

    private fun observeBookingsVisibilityForSite(siteModel: SiteModel): Flow<Boolean> = flow {
        val isCIABSite = featureFlagRepository.isEnabled(FeatureFlag.BOOKINGS_MVP) && siteModel.isCIABSite()
        if (!isCIABSite) {
            emit(false)
        } else {
            emitAll(
                bookableProductCountFlow()
                    .combine(bookingsCountFlow()) { productCount, bookingCount ->
                        productCount > 0 || bookingCount > 0
                    }
                    .distinctUntilChanged()
            )
        }
    }

    private fun bookableProductCountFlow(): Flow<Long> {
        return productListRepository.observeProductsCount(
            filterOptions = mapOf(
                ProductFilterOption.STATUS to ProductStatus.PUBLISH.value,
                ProductFilterOption.TYPE to ProductType.BOOKABLE_SERVICE.value
            ),
            excludeSampleProducts = true
        ).onFirst { count ->
            if (count == 0L) {
                appCoroutineScope.launch {
                    productListRepository.fetchProductList(
                        productFilterOptions = mapOf(ProductFilterOption.TYPE to ProductType.BOOKABLE_SERVICE.value)
                    ).onFailure {
                        WooLog.w(WooLog.T.BOOKINGS, "Failed to fetch bookable products")
                    }
                }
            }
        }
    }

    private fun bookingsCountFlow(): Flow<Long> {
        return bookingsRepository.observeBookingsCount()
            .onFirst { count ->
                if (count == 0L) {
                    appCoroutineScope.launch {
                        bookingsRepository.fetchBookings(
                            page = 1,
                            perPage = 25,
                            order = BookingsOrderOption.DESC
                        ).onFailure {
                            WooLog.w(WooLog.T.BOOKINGS, "Failed to fetch bookings")
                        }
                    }
                }
            }
    }
}
