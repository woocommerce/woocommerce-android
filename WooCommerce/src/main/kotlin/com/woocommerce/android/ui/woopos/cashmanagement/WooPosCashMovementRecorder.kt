package com.woocommerce.android.ui.woopos.cashmanagement

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the Core movement request before retrying it, so a completed order is never retried. */
@Singleton
class WooPosCashMovementRecorder @Inject constructor(
    @ApplicationContext context: Context,
    private val sessions: WooPosCashSessionRepository,
) {
    private val preferences = context.getSharedPreferences("woo_pos_cash_movements", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val queueLock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun captureSession(): WooPosCapturedCashSession? = try {
        sessions.currentWithSite()
    } catch (_: CashSessionUnsupportedException) {
        null
    }

    fun recordSale(capture: WooPosCapturedCashSession?, orderId: Long) {
        if (capture == null) return
        enqueue(PendingMovement("sale", capture.siteLocalId, capture.session.id,
            orderId, null, UUID.randomUUID().toString()))
        scope.launch { retryPending() }
    }

    fun recordRefund(capture: WooPosCapturedCashSession?, orderId: Long, refundId: Long) {
        if (capture == null) return
        enqueue(PendingMovement("refund", capture.siteLocalId, capture.session.id,
            orderId, refundId, UUID.randomUUID().toString()))
        scope.launch { retryPending() }
    }

    fun hasPending(sessionId: Long): Boolean {
        val siteLocalId = runCatching { sessions.selectedSiteLocalId }.getOrNull() ?: return false
        return pending().any { it.sessionId == sessionId && it.siteLocalId == siteLocalId }
    }

    suspend fun retryPending() {
        mutex.withLock {
            val selectedSiteLocalId = runCatching { sessions.selectedSiteLocalId }.getOrNull() ?: return@withLock
            pending().forEach { movement ->
                if (movement.siteLocalId != selectedSiteLocalId) return@forEach
                val recorded = try {
                    if (movement.type == "sale") {
                        sessions.sale(movement.sessionId, movement.orderId,
                            UUID.fromString(movement.requestId), movement.siteLocalId)
                    } else {
                        sessions.refund(
                            movement.sessionId, movement.orderId, requireNotNull(movement.refundId),
                            UUID.fromString(movement.requestId), movement.siteLocalId
                        )
                    }
                    true
                } catch (error: CashSessionException) {
                    error.code == "woocommerce_rest_cash_source_already_recorded" &&
                        error.recordedSessionId == movement.sessionId
                } catch (_: Exception) {
                    false
                }
                if (recorded) {
                    synchronized(queueLock) {
                        val updated = pending().filterNot { it.requestId == movement.requestId }
                        save(updated)
                    }
                }
            }
        }
    }

    private fun enqueue(movement: PendingMovement) = synchronized(queueLock) {
        val existing = pending()
        val alreadyQueued = existing.any {
            it.siteLocalId == movement.siteLocalId && it.type == movement.type &&
                it.orderId == movement.orderId && it.refundId == movement.refundId
        }
        if (!alreadyQueued) save(existing + movement)
    }

    private fun pending(): List<PendingMovement> = runCatching {
        val array = JSONArray(preferences.getString("pending", "[]"))
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            PendingMovement(
                item.getString("type"), item.optInt("siteLocalId", -1),
                item.getLong("sessionId"), item.getLong("orderId"),
                item.optLong("refundId").takeIf { item.has("refundId") }, item.getString("requestId")
            )
        }
    }.getOrDefault(emptyList())

    private fun save(movements: List<PendingMovement>) {
        val array = JSONArray()
        movements.forEach { item ->
            array.put(JSONObject().apply {
                put("type", item.type)
                put("siteLocalId", item.siteLocalId)
                put("sessionId", item.sessionId)
                put("orderId", item.orderId)
                item.refundId?.let { put("refundId", it) }
                put("requestId", item.requestId)
            })
        }
        preferences.edit().putString("pending", array.toString()).commit()
    }

    private data class PendingMovement(
        val type: String,
        val siteLocalId: Int,
        val sessionId: Long,
        val orderId: Long,
        val refundId: Long?,
        val requestId: String,
    )
}

data class WooPosCapturedCashSession(val session: WooPosCashSession, val siteLocalId: Int)
