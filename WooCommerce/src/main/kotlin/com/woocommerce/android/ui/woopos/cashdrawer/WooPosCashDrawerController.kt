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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    suspend fun connectPrinter(printer: WooPosReceiptPrinter): Result<Unit> = runCatching {
        val previous = hardware.selectedPrinter.value
        hardware.connect(printer)
        if (previous != printer) {
            synchronized(sensorLock) {
                openSignal = null
                lastSignal = null
                pendingOpen = null
            }
            preferences.edit().remove(OPEN_SIGNAL).apply()
        }
    }

    suspend fun disconnectPrinter() {
        hardware.disconnect()
        synchronized(sensorLock) {
            openSignal = null
            lastSignal = null
            pendingOpen = null
        }
        preferences.edit().remove(OPEN_SIGNAL).apply()
    }

    suspend fun printCloseOut(text: String): Result<Unit> = runCatching { hardware.printCloseOut(text) }

    private val sensorLock = Any()
    private var openSignal: Boolean? = preferences.getString(OPEN_SIGNAL, null)?.toBooleanStrictOrNull()
    private var lastSignal: Boolean? = null
    private var pendingOpen: PendingOpen? = null

    init {
        hardwareScope.launch { hardware.drawerSignals.collect { signal -> handleDrawerSignal(signal) } }
    }

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
        val boundSessionId = session?.takeIf { !it.drawerId.isNullOrBlank() }?.id
        return requestOpen(reason, boundSessionId, orderId)
    }

    /** A confirmed cash transaction passes the session ID captured before its Core movement. */
    @Suppress("UNUSED_PARAMETER")
    suspend fun openAutomatically(
        reason: WooPosCashDrawerReason,
        sessionId: Long?,
        sessionDrawerId: String?,
        configuredDrawerNameAtCapture: String?,
        orderId: Long? = null,
    ): WooPosCashDrawerOpenResult {
        if (!_autoOpen.value || sessionId == null) {
            return WooPosCashDrawerOpenResult.NO_SESSION
        }
        val boundSessionId = sessionId.takeIf { !sessionDrawerId.isNullOrBlank() }
        return requestOpen(reason, boundSessionId, orderId)
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

    private suspend fun requestOpen(
        reason: WooPosCashDrawerReason,
        boundSessionId: Long?,
        orderId: Long?,
    ): WooPosCashDrawerOpenResult {
        val correlationId = UUID.randomUUID()
        val pending = PendingOpen(reason, boundSessionId, orderId, correlationId, System.currentTimeMillis())
        synchronized(sensorLock) { pendingOpen = pending }
        val result = try {
                hardware.open()
                WooPosCashDrawerOpenResult.OPEN_REQUESTED
            } catch (_: WooPosPrinterNotConnectedException) {
                WooPosCashDrawerOpenResult.NOT_CONNECTED
            } catch (_: Exception) {
                WooPosCashDrawerOpenResult.FAILED
        }
        if (result != WooPosCashDrawerOpenResult.OPEN_REQUESTED) {
            synchronized(sensorLock) { if (pendingOpen === pending) pendingOpen = null }
        }
        if (boundSessionId != null) {
            try {
                sessions.drawerEvent(
                    id = boundSessionId,
                    type = if (result == WooPosCashDrawerOpenResult.OPEN_REQUESTED) "open_requested" else "open_failed",
                    reason = reason.apiValue,
                    orderId = orderId,
                    correlationId = correlationId,
                )
            } catch (_: Exception) {
                // Hardware outcome must not reverse an already confirmed cash transaction.
            }
        }
        if (result == WooPosCashDrawerOpenResult.OPEN_REQUESTED) {
            val earlySignal = synchronized(sensorLock) {
                if (pendingOpen !== pending) return@synchronized null
                pending.confirmed = true
                pending.earlySignal
            }
            if (earlySignal != null) recordConfirmedSignal(pending, earlySignal)
        }
        return result
    }

    internal suspend fun handleDrawerSignal(signal: Boolean) {
        val event = synchronized(sensorLock) {
            val previous = lastSignal
            if (previous == signal) return@synchronized null
            lastSignal = signal
            val pending = pendingOpen?.takeIf { System.currentTimeMillis() - it.atMillis <= SENSOR_WINDOW_MS }
            if (previous == null && pending == null) return@synchronized null
            if (pending != null && !pending.confirmed) {
                pending.earlySignal = signal
                return@synchronized null
            }
            resolveOpenSignal(signal, pending)
        }
        when (event) {
            is SensorEvent.Requested -> recordSensorEvent(event.pending)
            SensorEvent.Unknown -> recordUnknownSensorEvent()
            null -> Unit
        }
    }

    private suspend fun recordConfirmedSignal(pending: PendingOpen, signal: Boolean) {
        val event = synchronized(sensorLock) {
            if (pendingOpen !== pending) return@synchronized null
            resolveOpenSignal(signal, pending)
        }
        if (event is SensorEvent.Requested) recordSensorEvent(event.pending)
    }

    private fun resolveOpenSignal(signal: Boolean, pending: PendingOpen?): SensorEvent? {
        if (openSignal == null) {
            if (pending == null) return null
            openSignal = signal
            preferences.edit().putString(OPEN_SIGNAL, signal.toString()).apply()
        }
        if (signal != openSignal) return null
        return if (pending != null) {
            pendingOpen = null
            SensorEvent.Requested(pending)
        } else {
            SensorEvent.Unknown
        }
    }

    private suspend fun recordSensorEvent(pending: PendingOpen) {
        val sessionId = pending.sessionId ?: return
        try {
            sessions.drawerEvent(sessionId, "opened", pending.reason.apiValue, pending.orderId, pending.correlationId)
        } catch (_: Exception) {
            // The captured session can close before the sensor confirms opening.
        }
    }

    private suspend fun recordUnknownSensorEvent() {
        val signalAt = Instant.now()
        val session = try { sessions.current() } catch (_: Exception) { null } ?: return
        if (session.status != "open" || session.drawerId.isNullOrBlank()) return
        val createdAt = parseCreatedAt(session.dateCreatedGmt) ?: return
        if (createdAt.isAfter(signalAt)) return
        try {
            sessions.drawerEvent(session.id, "opened", "unknown", null, null)
        } catch (_: Exception) {
            // A session closed while the sensor event was being checked.
        }
    }

    private fun parseCreatedAt(value: String): Instant? = try {
        Instant.parse(value)
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (_: Exception) {
            try { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) } catch (_: Exception) { null }
        }
    }

    private class PendingOpen(
        val reason: WooPosCashDrawerReason,
        val sessionId: Long?,
        val orderId: Long?,
        val correlationId: UUID,
        val atMillis: Long,
        var confirmed: Boolean = false,
        var earlySignal: Boolean? = null,
    )

    private sealed interface SensorEvent {
        data class Requested(val pending: PendingOpen) : SensorEvent
        data object Unknown : SensorEvent
    }

    companion object {
        private const val DRAWER_NAME = "drawer_name"
        private const val AUTO_OPEN = "auto_open"
        private const val OPEN_SIGNAL = "open_signal"
        private const val SENSOR_WINDOW_MS = 5_000L
    }
}
