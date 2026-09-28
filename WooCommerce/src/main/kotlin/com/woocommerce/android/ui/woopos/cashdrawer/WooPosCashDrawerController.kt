package com.woocommerce.android.ui.woopos.cashdrawer

import android.content.Context
import com.woocommerce.android.ui.woopos.cashmanagement.WooPosCashSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class WooPosCashDrawerReason(val apiValue: String) {
    CASH_SALE("cash_sale"),
    CASH_REFUND("cash_refund"),
    NO_SALE("no_sale"),
    TEST("test"),
}

enum class WooPosCashDrawerOpenResult {
    OPEN_REQUESTED,
    NO_SESSION,
    NOT_CONNECTED,
    FAILED,
}

@Singleton
class WooPosCashDrawerController @Inject constructor(
    @ApplicationContext context: Context,
    private val hardware: WooPosCashDrawerHardware,
    private val sessions: WooPosCashSessionRepository,
) {
    private val preferences = context.getSharedPreferences("woo_pos_cash_drawer", Context.MODE_PRIVATE)
    private val hardwareScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _drawerName = MutableStateFlow(preferences.getString(DRAWER_NAME, null))
    val drawerNameFlow: StateFlow<String?> = _drawerName.asStateFlow()
    val drawerName: String? get() = _drawerName.value

    private val _autoOpen = MutableStateFlow(preferences.getBoolean(AUTO_OPEN, true))
    val autoOpenFlow: StateFlow<Boolean> = _autoOpen.asStateFlow()
    val autoOpen: Boolean get() = _autoOpen.value
    val isConnected: StateFlow<Boolean> get() = hardware.isConnected
    val selectedPrinter: StateFlow<WooPosReceiptPrinter?> get() = hardware.selectedPrinter

    suspend fun discoverPrinters(): Result<List<WooPosReceiptPrinter>> = runCatching { hardware.discover() }

    suspend fun connectPrinter(printer: WooPosReceiptPrinter): Result<Unit> = runCatching { hardware.connect(printer) }

    suspend fun disconnectPrinter() = hardware.disconnect()

    suspend fun printCloseOut(text: String): Result<Unit> = runCatching { hardware.printCloseOut(text) }

    fun setDrawerName(name: String) {
        val trimmed = name.trim().takeIf { it.isNotEmpty() }
        _drawerName.value = trimmed
        preferences.edit().putString(DRAWER_NAME, trimmed).apply()
    }

    fun setAutoOpen(enabled: Boolean) {
        _autoOpen.value = enabled
        preferences.edit().putBoolean(AUTO_OPEN, enabled).apply()
    }

    /** The cashier can count the float before a session exists. This attempt has no Core event. */
    suspend fun openBeforeSession(): WooPosCashDrawerOpenResult = requestOpen(WooPosCashDrawerReason.NO_SALE, null, null)

    /** Test opens are allowed without a session. Other manual opens require an active session. */
    suspend fun open(reason: WooPosCashDrawerReason, orderId: Long? = null): WooPosCashDrawerOpenResult {
        val session = try {
            sessions.current()
        } catch (_: Exception) {
            null
        }
        if (session == null && reason != WooPosCashDrawerReason.TEST) return WooPosCashDrawerOpenResult.NO_SESSION
        if (session != null && reason != WooPosCashDrawerReason.TEST && !isBoundToConfiguredDrawer(session.drawerId)) {
            return WooPosCashDrawerOpenResult.NO_SESSION
        }
        val boundSessionId = session?.takeIf { isBoundToConfiguredDrawer(it.drawerId) }?.id
        return requestOpen(reason, boundSessionId, orderId)
    }

    /** A confirmed cash transaction passes the session ID captured before its Core movement. */
    suspend fun openAutomatically(
        reason: WooPosCashDrawerReason,
        sessionId: Long?,
        sessionDrawerId: String?,
        configuredDrawerNameAtCapture: String?,
        orderId: Long? = null,
    ): WooPosCashDrawerOpenResult {
        if (!_autoOpen.value || sessionId == null || sessionDrawerId.isNullOrBlank() ||
            !sessionDrawerId.trim().equals(configuredDrawerNameAtCapture?.trim(), ignoreCase = true)
        ) {
            return WooPosCashDrawerOpenResult.NO_SESSION
        }
        return requestOpen(reason, sessionId, orderId)
    }

    fun scheduleAutomaticOpen(
        reason: WooPosCashDrawerReason,
        sessionId: Long?,
        sessionDrawerId: String?,
        configuredDrawerNameAtCapture: String?,
        orderId: Long? = null,
    ) {
        hardwareScope.launch {
            openAutomatically(reason, sessionId, sessionDrawerId, configuredDrawerNameAtCapture, orderId)
        }
    }

    private fun isBoundToConfiguredDrawer(boundName: String?): Boolean {
        val configuredName = drawerName ?: return false
        return boundName?.trim()?.equals(configuredName, ignoreCase = true) == true
    }

    private suspend fun requestOpen(
        reason: WooPosCashDrawerReason,
        boundSessionId: Long?,
        orderId: Long?,
    ): WooPosCashDrawerOpenResult {
        val result = try {
                hardware.open()
                WooPosCashDrawerOpenResult.OPEN_REQUESTED
            } catch (_: WooPosPrinterNotConnectedException) {
                WooPosCashDrawerOpenResult.NOT_CONNECTED
            } catch (_: Exception) {
                WooPosCashDrawerOpenResult.FAILED
        }
        if (boundSessionId != null) {
            try {
                sessions.drawerEvent(
                    id = boundSessionId,
                    type = if (result == WooPosCashDrawerOpenResult.OPEN_REQUESTED) "open_requested" else "open_failed",
                    reason = reason.apiValue,
                    orderId = orderId,
                    correlationId = UUID.randomUUID(),
                )
            } catch (_: Exception) {
                // Hardware outcome must not reverse an already confirmed cash transaction.
            }
        }
        return result
    }

    companion object {
        private const val DRAWER_NAME = "drawer_name"
        private const val AUTO_OPEN = "auto_open"
    }
}
