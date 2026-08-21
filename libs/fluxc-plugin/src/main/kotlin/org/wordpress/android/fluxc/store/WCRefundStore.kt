package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient
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
    private val previewRestClient: RefundPreviewRestClient,
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

    /**
     * Fetches a server-calculated refund preview via `POST /wc/v3/orders/<order_id>/refunds/preview`.
     *
     * On a store whose WooCommerce does not register the preview route yet, the result is an error
     * with [WooErrorType.API_NOT_FOUND]; callers should detect this and fall back to the
     * local-calculation flow.
     */
    suspend fun previewRefund(
        site: SiteModel,
        orderId: Long,
        items: List<RefundPreviewLineItem>,
    ): WooResult<WCRefundPreview> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "previewRefund") {
            val response = previewRestClient.previewRefund(site, orderId, items)
            return@withDefaultContext when {
                response.isError -> WooResult(response.error)
                response.result != null -> WooResult(refundsMapper.toPreviewModel(response.result))
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }

    /**
     * Creates a server-computed refund through `POST /wc/v3/orders/<order_id>/refunds` with
     * `compute_totals=true`. The client sends the items, the server computes the money. [amount]
     * is an optional override: below the computed total the store returns 400, above it the store
     * accepts an over-refund up to the order's remaining refundable amount.
     *
     * A store without `compute_totals` drops the param and books a zero-amount refund with a
     * restock, and still answers 201. Call this only after [previewRefund] succeeded on a store
     * running WooCommerce 11.1.0 or newer. POS checks both in `WooPosResolveRefundFlow`.
     *
     * No parameter has a default: each one moves the merchant's money or stock.
     */
    @Suppress("LongParameterList")
    suspend fun createComputedItemsRefund(
        site: SiteModel,
        orderId: Long,
        reason: String,
        autoRefund: Boolean,
        restockItems: Boolean,
        amount: BigDecimal?,
        items: List<ComputedRefundLineItem>,
    ): WooResult<WCRefundModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createComputedItemsRefund") {
            val response = restClient.createComputedRefund(
                site = site,
                orderId = orderId,
                reason = reason,
                apiRefund = autoRefund,
                apiRestock = restockItems,
                // toPlainString: toString() can emit scientific notation for extreme scales.
                amount = amount?.toPlainString(),
                lineItems = items,
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
                    val entities = response.result.map { refundResponse ->
                        refundsMapper.toEntity(
                            siteId = site.localId(),
                            orderId = RemoteId(orderId),
                            refundResponse = refundResponse
                        )
                    }
                    refundDao.replaceRefundsForOrder(site.localId(), RemoteId(orderId), entities)

                    WooResult(response.result.map(refundsMapper::toModel))
                }
                else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }
        }
    }
}
