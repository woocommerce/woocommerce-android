package com.woocommerce.android.ui.coupons

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmssGmt
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.coupons.tracking.StoreManagementCouponCreationFlowTrackerEventProvider
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFlowTrackerEventProvider
import com.woocommerce.android.util.WooLog
import com.woocommerce.commons.stats.StatsTimeRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val posTrackingEventProvider: WooPosCouponCreationFlowTrackerEventProvider,
    private val storeManagementTrackingEventProvider: StoreManagementCouponCreationFlowTrackerEventProvider,
) {
    suspend fun fetchCoupons(
        page: Int,
        pageSize: Int,
        couponIds: List<Long> = emptyList(),
        isPosMode: Boolean = false
    ): Result<Boolean> {
        return store.fetchCoupons(
            site = selectedSite.get(),
            page = page,
            pageSize = pageSize,
            couponIds = couponIds,
            deleteOldData = page == 1 && couponIds.isEmpty()
        )
            .let { result ->
                if (result.isError) {
                    analyticsTrackerWrapper.track(
                        if (isPosMode) {
                            posTrackingEventProvider.COUPONS_LOAD_FAILED
                        } else {
                            storeManagementTrackingEventProvider.COUPONS_LOAD_FAILED
                        },
                        mapOf(
                            AnalyticsTracker.KEY_ERROR_CONTEXT to result.error::class.java.simpleName,
                            AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.name,
                            AnalyticsTracker.KEY_ERROR_DESC to result.error.message
                        )
                    )

                    WooLog.w(
                        WooLog.T.COUPONS,
                        "Fetching coupons failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    trackCouponsLoaded(isPosMode, page)
                    Result.success(result.model!!)
                }
            }
    }

    private fun trackCouponsLoaded(isPosMode: Boolean, page: Int) {
        if (!isPosMode) {
            analyticsTrackerWrapper.track(
                storeManagementTrackingEventProvider.COUPONS_LOADED,
                mapOf(Pair(AnalyticsTracker.KEY_IS_LOADING_MORE, page > 1))
            )
        }
    }

    suspend fun searchCoupons(
        searchString: String,
        page: Int,
        pageSize: Int
    ): Result<SearchResult> {
        return store.searchCoupons(
            selectedSite.get(),
            searchString = searchString,
            page = page,
            pageSize = pageSize
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.COUPONS,
                    "Searching coupons failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                Result.success(
                    SearchResult(
                        coupons = searchResult.coupons.map { couponDataModel -> couponDataModel.toAppModel() },
                        canLoadMore = searchResult.canLoadMore
                    )
                )
            }
        }
    }

    fun observeCoupons(couponIds: List<Long> = emptyList()): Flow<List<Coupon>> = store.observeCoupons(
        site = selectedSite.get(),
        couponIds = couponIds
    ).map {
        it.map { couponDataModel -> couponDataModel.toAppModel() }
    }

    fun observeCoupon(couponId: Long): Flow<Coupon> = store.observeCoupon(selectedSite.get(), couponId)
        .filterNotNull()
        .map { it.toAppModel() }

    suspend fun fetchCoupon(couponId: Long): Result<Unit> {
        val result = store.fetchCoupon(selectedSite.get(), couponId)
        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun fetchCouponPerformance(couponId: Long): Result<CouponPerformanceReport> {
        val result = store.fetchCouponReport(selectedSite.get(), couponId)

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model!!.toAppModel())
        }
    }

    suspend fun fetchMostActiveCoupons(
        dateRange: StatsTimeRange,
        limit: Int
    ): Result<List<CouponPerformanceReport>> {
        val result = store.fetchMostActiveCoupons(
            site = selectedSite.get(),
            dateRange = dateRange.start..dateRange.end,
            limit = limit
        )

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model!!.map { it.toAppModel() })
        }
    }

    suspend fun deleteCoupon(couponId: Long): Result<Unit> {
        val result = store.deleteCoupon(
            site = selectedSite.get(),
            couponId = couponId,
            trash = false
        )

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun updateCoupon(coupon: Coupon): Result<Unit> {
        val request = coupon.createUpdateCouponRequest()

        val result = store.updateCoupon(
            site = selectedSite.get(),
            couponId = coupon.id,
            updateCouponRequest = request
        )

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(Unit)
        }
    }

    suspend fun createCoupon(coupon: Coupon): Result<Long?> {
        val request = coupon.createUpdateCouponRequest()

        val result = store.createCoupon(
            site = selectedSite.get(),
            updateCouponRequest = request
        )

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(result.model)
        }
    }

    suspend fun getCoupons(couponIds: List<Long>): List<Coupon> {
        return store.getCoupons(selectedSite.get(), couponIds).map { it.toAppModel() }
    }

    private fun Coupon.createUpdateCouponRequest() =
        UpdateCouponRequest(
            code = code,
            description = description,
            amount = amount?.toPlainString(),
            discountType = type?.value,
            isShippingFree = isShippingFree,
            expiryDate = dateExpires?.formatToYYYYmmDDhhmmssGmt() ?: "",
            productIds = productIds,
            productCategoryIds = categoryIds,
            usageLimit = restrictions.usageLimit,
            usageLimitPerUser = restrictions.usageLimitPerUser,
            restrictedEmails = restrictions.restrictedEmails,
            areSaleItemsExcluded = restrictions.areSaleItemsExcluded,
            isForIndividualUse = restrictions.isForIndividualUse,
            maximumAmount = restrictions.maximumAmount?.toPlainString(),
            minimumAmount = restrictions.minimumAmount?.toPlainString(),
            limitUsageToXItems = restrictions.limitUsageToXItems,
            excludedProductIds = restrictions.excludedProductIds,
            excludedProductCategoryIds = restrictions.excludedCategoryIds
        )

    data class SearchResult(
        val coupons: List<Coupon>,
        val canLoadMore: Boolean
    )
}
