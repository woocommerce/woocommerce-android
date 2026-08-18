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
     * `compute_totals=true`. The client sends only the items being refunded and the server
     * computes every monetary value. There is no way to send a total: the endpoint accepts one as
     * an override, but exposing it would let a caller decide what the refund is worth, which is
     * the opposite of what this method is for. Use [createItemsRefund] for the locally calculated
     * flow.
     *
     * On a store whose refund endpoint does not support `compute_totals`, the unknown param is
     * silently dropped and the request is handled by the classic create, where a quantity-only
     * body carries no monetary value to sum: the store books a zero-amount refund, restocks the
     * items, and answers 201, so the caller is told it succeeded.
     *
     * Two conditions must hold before calling this, and a successful [previewRefund] covers only
     * the second. The site's WooCommerce version must be known to support the `compute_totals`
     * create, and a preview for this exact selection must have succeeded. A preview on its own is
     * not enough — the preview route and the `compute_totals` create shipped as separate
     * WooCommerce changes, so a preview proves only that the preview route exists. POS enforces
     * both in `WooPosResolveRefundFlow`.
     *
     * No parameter has a default. [reason], [autoRefund] and [restockItems] each change what the
     * store does with the merchant's money or stock, so a caller has to say what it wants rather
     * than inherit it.
     */
    suspend fun createComputedItemsRefund(
        site: SiteModel,
        orderId: Long,
        reason: String,
        autoRefund: Boolean,
        restockItems: Boolean,
        items: List<ComputedRefundLineItem>,
    ): WooResult<WCRefundModel> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "createComputedItemsRefund") {
            val response = restClient.createComputedRefund(
                site = site,
                orderId = orderId,
                reason = reason,
                apiRefund = autoRefund,
                apiRestock = restockItems,
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
