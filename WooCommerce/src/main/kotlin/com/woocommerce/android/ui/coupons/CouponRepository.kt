package com.woocommerce.android.ui.coupons

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.model.CouponPerformanceReport
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.store.CouponStore
import javax.inject.Inject

class CouponRepository @Inject constructor(
    private val store: CouponStore,
    private val selectedSite: SelectedSite,
    private val dateUtils: DateUtils
) {
    suspend fun fetchCoupons(
        page: Int,
        pageSize: Int
    ): Result<Boolean> {
        return store.fetchCoupons(selectedSite.get(), page, pageSize)
            .let { result ->
                if (result.isError) {
                    AnalyticsTracker.track(
                        AnalyticsEvent.COUPONS_LOAD_FAILED,
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
                    AnalyticsTracker.track(
                        AnalyticsEvent.COUPONS_LOADED,
                        mapOf(Pair(AnalyticsTracker.KEY_IS_LOADING_MORE, page > 1))
                    )
                    Result.success(result.model!!)
                }
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

    fun observeCoupons(): Flow<List<Coupon>> = store.observeCoupons(selectedSite.get()).map {
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
        val request = UpdateCouponRequest(
            code = coupon.code,
            description = coupon.description,
            amount = coupon.amount?.toPlainString(),
            discountType = coupon.type?.value,
            isShippingFree = coupon.isShippingFree,
            expiryDate = coupon.dateExpires?.time?.let { dateUtils.toIso8601Format(it) } ?: "",
            productIds = coupon.productIds,
            productCategoryIds = coupon.categoryIds,
            usageLimit = coupon.restrictions.usageLimit,
            usageLimitPerUser = coupon.restrictions.usageLimitPerUser,
            restrictedEmails = coupon.restrictions.restrictedEmails,
            areSaleItemsExcluded = coupon.restrictions.areSaleItemsExcluded,
            isForIndividualUse = coupon.restrictions.isForIndividualUse,
            maximumAmount = coupon.restrictions.maximumAmount?.toPlainString(),
            minimumAmount = coupon.restrictions.minimumAmount?.toPlainString(),
            limitUsageToXItems = coupon.restrictions.limitUsageToXItems,
            excludedProductIds = coupon.restrictions.excludedProductIds,
            excludedProductCategoryIds = coupon.restrictions.excludedCategoryIds
        )

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

    data class SearchResult(
        val coupons: List<Coupon>,
        val canLoadMore: Boolean
    )
}
