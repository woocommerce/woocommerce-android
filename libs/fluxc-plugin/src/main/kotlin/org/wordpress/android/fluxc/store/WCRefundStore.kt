package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.dao.RefundDao
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCRefundStore @Inject internal constructor(
    private val restClient: RefundRestClient,
    private val coroutineEngine: CoroutineEngine,
    private val refundsMapper: RefundMapper,
    private val refundDao: RefundDao,
) {
    companion object {
        // Just get everything
        const val DEFAULT_PAGE_SIZE = 100
        const val DEFAULT_PAGE = 1
    }

    suspend fun createAmountRefund(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal,
        reason: String = "",
        autoRefund: Boolean = false
    ): WooResult<WCRefundModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createAmountRefund") {
            val response = restClient.createRefundByAmount(
                    site,
                    orderId,
                    amount.toString(),
                    reason,
                    autoRefund
            )
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(refundsMapper.toModel(response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun createItemsRefund(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal?,
        reason: String = "",
        restockItems: Boolean = true,
        autoRefund: Boolean = false,
        items: List<RefundRequestItem>
    ): WooResult<WCRefundModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createItemsRefund") {
            val response = restClient.createRefundByItems(
                site = site,
                orderId = orderId,
                amount = amount,
                reason = reason,
                automaticRefund = autoRefund,
                items = items,
                restockItems = restockItems
            )
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(refundsMapper.toModel(response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun getRefund(site: SiteModel, orderId: Long, refundId: Long): WCRefundModel? {
        return refundDao.getRefund(
            siteId = site.localId(),
            orderId = RemoteId(orderId),
            refundId = RemoteId(refundId)
        )?.let(refundsMapper::toModel)
    }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): WooResult<WCRefundModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchRefund") {
            val response = restClient.fetchRefund(site, orderId, refundId)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    response.result.let {
                        refundsMapper.toEntity(
                            siteId = site.localId(),
                            orderId = RemoteId(orderId),
                            refundResponse = it
                        )
                    }.let {
                        refundDao.upsertRefund(it)
                    }
                    WooResult(refundsMapper.toModel(response.result))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    suspend fun getAllRefunds(site: SiteModel, orderId: Long): List<WCRefundModel> {
        val entities = refundDao.getRefundsForOrder(site.localId(), RemoteId(orderId))
        return entities.map(refundsMapper::toModel)
    }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long,
        page: Int = DEFAULT_PAGE,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): WooResult<List<WCRefundModel>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchAllRefunds") {
            val response = restClient.fetchAllRefunds(
                    site,
                    orderId,
                    page,
                    pageSize
            )
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> {
                    refundDao.deleteRefundsForOrder(site.localId(), RemoteId(orderId))

                    response.result.map { refundResponse ->
                        refundsMapper.toEntity(
                            siteId = site.localId(),
                            orderId = RemoteId(orderId),
                            refundResponse = refundResponse
                        )
                    }.let {
                        refundDao.upsertRefunds(it)
                    }

                    WooResult(response.result.map(refundsMapper::toModel))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
