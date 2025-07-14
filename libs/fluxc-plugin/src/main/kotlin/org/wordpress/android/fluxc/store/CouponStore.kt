package org.wordpress.android.fluxc.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.coupon.UpdateCouponRequest
import org.wordpress.android.fluxc.model.coupons.CouponReport
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.CouponRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.coupons.toDataModel
import org.wordpress.android.fluxc.persistence.TransactionExecutor
import org.wordpress.android.fluxc.persistence.dao.CouponsDao
import org.wordpress.android.fluxc.persistence.entity.CouponEmailEntity
import org.wordpress.android.fluxc.persistence.entity.CouponWithEmails
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.API
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouponStore @Inject constructor(
    private val restClient: CouponRestClient,
    private val couponsDao: CouponsDao,
    private val coroutineEngine: CoroutineEngine,
    private val database: TransactionExecutor
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    // Returns a boolean indicating whether more coupons can be fetched
    suspend fun fetchCoupons(
        site: SiteModel,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        couponIds: List<Long> = emptyList(),
        deleteOldData: Boolean = page == 1
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupons") {
            val response = restClient.fetchCoupons(
                site = site,
                page = page,
                pageSize = pageSize,
                couponIds = couponIds
            )
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    database.executeInTransaction {
                        if (deleteOldData) {
                            couponsDao.deleteAllCoupons(site.localId())
                        }

                        response.result.forEach { addCouponToDatabase(it, site) }
                    }
                    val canLoadMore = response.result.size == pageSize
                    WooResult(canLoadMore)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun searchCoupons(
        site: SiteModel,
        searchString: String,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<CouponSearchResult> {
        return coroutineEngine.withDefaultContext(API, this, "searchCoupons") {
            val response = restClient.fetchCoupons(site, page, pageSize, searchString)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    database.executeInTransaction {
                        response.result.forEach { addCouponToDatabase(it, site) }
                    }

                    val couponIds = response.result.map { RemoteId(it.id) }
                    val coupons = couponsDao.getCoupons(site.localId(), couponIds)
                    val canLoadMore = response.result.size == pageSize
                    WooResult(CouponSearchResult(coupons, canLoadMore))
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun fetchCoupon(site: SiteModel, couponId: Long): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(API, this, "fetchCoupon") {
            val response = restClient.fetchCoupon(site, couponId)
            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    addCouponToDatabase(response.result, site)
                    WooResult(Unit)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    private suspend fun addCouponToDatabase(dto: CouponDto, site: SiteModel) {
        database.executeInTransaction {
            couponsDao.insertOrUpdateCoupon(dto.toDataModel(site.localId()))
            insertRestrictedEmailAddresses(dto, site)
        }
    }

    suspend fun deleteCoupon(
        site: SiteModel,
        couponId: Long,
        trash: Boolean = true
    ): WooResult<Unit> {
        return coroutineEngine.withDefaultContext(T.API, this, "deleteCoupon") {
            val result = restClient.deleteCoupon(site, couponId, trash)

            return@withDefaultContext if (result.isError) {
                WooResult(result.error)
            } else {
                couponsDao.deleteCoupon(site.localId(), RemoteId(couponId))
                WooResult(Unit)
            }
        }
    }

    private suspend fun insertRestrictedEmailAddresses(dto: CouponDto, site: SiteModel) {
        dto.restrictedEmails?.forEach { email ->
            couponsDao.insertOrUpdateCouponEmail(
                CouponEmailEntity(
                    couponId = RemoteId(dto.id),
                    localSiteId = site.localId(),
                    email = email
                )
            )
        }
    }

    fun observeCoupon(site: SiteModel, couponId: Long) =
        couponsDao.observeCoupon(site.localId(), RemoteId(couponId))
            .distinctUntilChanged()

    suspend fun getCoupon(site: SiteModel, couponId: Long) =
        couponsDao.getCoupon(site.localId(), RemoteId(couponId))

    fun observeCoupons(
        site: SiteModel,
        couponIds: List<Long> = emptyList()
    ): Flow<List<CouponWithEmails>> {
        return if (couponIds.isEmpty()) {
            couponsDao.observeCoupons(site.localId())
        } else {
            couponsDao.observeCoupons(site.localId(), couponIds)
        }.distinctUntilChanged()
    }

    suspend fun fetchCouponReport(site: SiteModel, couponId: Long): WooResult<CouponReport> =
        coroutineEngine.withDefaultContext(T.API, this, "fetchCouponReport") {
            // Old date, 1 second since epoch
            val date = Date(TimeUnit.SECONDS.toMillis(1))

            return@withDefaultContext restClient.fetchCouponReport(site, couponId, date)
                .let { result ->
                    if (result.isError) {
                        WooResult(result.error)
                    } else {
                        WooResult(result.result!!.toDataModel())
                    }
                }
        }

    suspend fun createCoupon(
        site: SiteModel,
        updateCouponRequest: UpdateCouponRequest
    ): WooResult<Long> =
        coroutineEngine.withDefaultContext(T.API, this, "createCoupon") {
            return@withDefaultContext restClient.createCoupon(site, updateCouponRequest)
                .let { response ->
                    if (response.isError || response.result == null) {
                        WooResult(response.error)
                    } else {
                        addCouponToDatabase(response.result, site)
                        WooResult(response.result.id)
                    }
                }
        }

    suspend fun updateCoupon(
        couponId: Long,
        site: SiteModel,
        updateCouponRequest: UpdateCouponRequest
    ): WooResult<Unit> {
        return if (updateCouponRequest.code.isNullOrEmpty()) {
            WooResult(
                WooError(
                    type = INVALID_PARAM,
                    original = UNKNOWN,
                    message = "Coupon code cannot be empty"
                )
            )
        } else {
            coroutineEngine.withDefaultContext(T.API, this, "createCoupon") {
                return@withDefaultContext restClient.updateCoupon(
                    site,
                    couponId,
                    updateCouponRequest
                )
                    .let { response ->
                        if (response.isError || response.result == null) {
                            WooResult(response.error)
                        } else {
                            addCouponToDatabase(response.result, site)
                            WooResult(Unit)
                        }
                    }
            }
        }
    }

    suspend fun fetchMostActiveCoupons(
        site: SiteModel,
        dateRange: ClosedRange<Date>,
        limit: Int
    ): WooResult<List<CouponReport>> {
        return coroutineEngine.withDefaultContext(T.API, this, "fetchMostRecentCoupons") {
            val response = restClient.fetchCouponsReports(
                site = site,
                before = dateRange.endInclusive,
                after = dateRange.start,
                perPage = limit,
                orderBy = CouponRestClient.CouponsReportOrderBy.OrdersCount,
            )

            when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    val reports = response.result.map { it.toDataModel() }
                    WooResult(reports)
                }

                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun getCoupons(site: SiteModel, couponIds: List<Long>): List<CouponWithEmails> {
        return couponsDao.getCoupons(site.localId(), couponIds.map { RemoteId(it) })
    }

    data class CouponSearchResult(
        val coupons: List<CouponWithEmails>,
        val canLoadMore: Boolean
    )
}
