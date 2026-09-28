package com.woocommerce.android.ui.woopos.cashmanagement

import android.content.Context
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCashSessionRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val network: WooNetwork,
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("woo_pos_cash_session", Context.MODE_PRIVATE)
    val deviceId: String = preferences.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        preferences.edit().putString("device_id", it).apply()
    }

    val selectedSiteLocalId: Int get() = selectedSite.get().id

    suspend fun current(): WooPosCashSession? = list("open", 1).items.firstOrNull()

    suspend fun currentWithSite(): WooPosCapturedCashSession? {
        val siteLocalId = selectedSiteLocalId
        val session = current()
        if (selectedSiteLocalId != siteLocalId) throw CashSessionSiteChangedException()
        return session?.let { WooPosCapturedCashSession(it, siteLocalId) }
    }

    suspend fun past(page: Int): WooPosCashPage<WooPosCashSession> = list("closed", page)

    private suspend fun list(status: String, page: Int): WooPosCashPage<WooPosCashSession> {
        val response = network.executeGetGsonRequest(
            selectedSite.get(), PATH, Array<WooPosCashSession>::class.java,
            mapOf("status" to status, "page" to page.toString(), "per_page" to "20") +
                if (status == "open") mapOf("device_id" to deviceId) else emptyMap()
        )
        val items = response.requireData().toList()
        val totalPages = (response as? WPAPIResponse.Success)?.headers
            ?.firstOrNull { it.key.equals("X-WP-TotalPages", ignoreCase = true) }?.value?.toIntOrNull()
        return WooPosCashPage(items, totalPages?.let { page < it } ?: (items.size == 20))
    }

    suspend fun detail(id: Long): WooPosCashSession = get("$PATH/$id", WooPosCashSession::class.java)

    suspend fun movements(id: Long, page: Int): WooPosCashPage<WooPosCashMovement> {
        val response = network.executeGetGsonRequest(
            selectedSite.get(), "$PATH/$id/movements", Array<WooPosCashMovement>::class.java,
            mapOf("page" to page.toString(), "per_page" to "50")
        )
        val items = response.requireData().toList()
        val totalPages = (response as? WPAPIResponse.Success)?.headers
            ?.firstOrNull { it.key.equals("X-WP-TotalPages", ignoreCase = true) }?.value?.toIntOrNull()
        return WooPosCashPage(items, totalPages?.let { page < it } ?: (items.size == 50))
    }

    suspend fun start(opening: BigDecimal, drawerName: String?, requestId: UUID): WooPosCashSession = post(
        PATH,
        mapOf("request_id" to requestId.toString(), "device_id" to deviceId,
            "opening_amount" to opening.toPlainString()) +
            (drawerName?.trim()?.takeIf { it.isNotEmpty() }?.let { mapOf("drawer_id" to it) } ?: emptyMap()),
        WooPosCashSession::class.java
    )

    suspend fun adjust(
        id: Long, type: String, amount: BigDecimal, reason: String, requestId: UUID, siteLocalId: Int
    ): WooPosCashMovement =
        post("$PATH/$id/movements", mapOf("request_id" to requestId.toString(), "type" to type,
            "amount" to amount.toPlainString(), "reason" to reason), WooPosCashMovement::class.java, siteLocalId)

    suspend fun sale(id: Long, orderId: Long, requestId: UUID, siteLocalId: Int): WooPosCashMovement =
        post("$PATH/$id/movements", mapOf("request_id" to requestId.toString(), "type" to "cash_sale",
            "order_id" to orderId), WooPosCashMovement::class.java, siteLocalId)

    suspend fun refund(id: Long, orderId: Long, refundId: Long, requestId: UUID, siteLocalId: Int): WooPosCashMovement =
        post("$PATH/$id/movements", mapOf("request_id" to requestId.toString(), "type" to "cash_refund",
            "order_id" to orderId, "refund_id" to refundId), WooPosCashMovement::class.java, siteLocalId)

    suspend fun drawerEvent(
        id: Long, type: String, reason: String, orderId: Long?, correlationId: UUID?, siteLocalId: Int
    ) {
        post("$PATH/$id/drawer-events", buildMap {
            put("request_id", UUID.randomUUID().toString())
            put("type", type)
            put("reason", reason)
            put("occurred_at", java.time.OffsetDateTime.now().toString())
            orderId?.let { put("order_id", it) }
            correlationId?.let { put("correlation_id", it.toString()) }
        }, DrawerEventResponse::class.java, siteLocalId)
    }

    suspend fun close(id: Long, revision: Int, counted: BigDecimal, note: String?, requestId: UUID): WooPosCashSession = post(
        "$PATH/$id/close",
        mapOf("request_id" to requestId.toString(), "expected_revision" to revision,
            "counted_amount" to counted.toPlainString()) +
            (note?.takeIf { it.isNotBlank() }?.let { mapOf("note" to it) } ?: emptyMap()),
        WooPosCashSession::class.java
    )

    private suspend fun <T : Any> get(path: String, clazz: Class<T>): T =
        network.executeGetGsonRequest(selectedSite.get(), path, clazz).requireData()

    private suspend fun <T : Any> post(
        path: String, body: Map<String, Any>, clazz: Class<T>, expectedSiteLocalId: Int? = null
    ): T {
        val site = selectedSite.get()
        if (expectedSiteLocalId != null && site.id != expectedSiteLocalId) throw CashSessionSiteChangedException()
        return network.executePostGsonRequest(site, path, clazz, body).requireData()
    }

    private fun <T> WPAPIResponse<T>.requireData(): T = when (this) {
        is WPAPIResponse.Success -> data ?: throw CashSessionException("The cash session response was empty")
        is WPAPIResponse.Error -> {
            if (error.errorCode == "rest_no_route") throw CashSessionUnsupportedException()
            throw CashSessionException(
                error.message ?: "The cash session request failed",
                error.errorCode,
                error.errorData?.optLong("session_id")
            )
        }
    }

    private data class DrawerEventResponse(val id: Long)

    companion object {
        private const val PATH = "wc/pos/v1/cash-sessions"
    }
}

class CashSessionException(
    message: String,
    val code: String? = null,
    val recordedSessionId: Long? = null,
) : Exception(message)

class CashSessionUnsupportedException : Exception("Update WooCommerce to use cash sessions")

class CashSessionSiteChangedException : Exception("The selected store changed")
