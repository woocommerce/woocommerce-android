package com.woocommerce.android.ui.woopos.cashmanagement

import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerController
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosGetCurrencyFormattingParameters
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WooPosCashManagementViewModel @Inject constructor(
    private val repository: WooPosCashSessionRepository,
    val drawer: WooPosCashDrawerController,
    private val recorder: WooPosCashMovementRecorder,
    private val resources: ResourceProvider,
    private val currencyFormatting: WooPosGetCurrencyFormattingParameters,
) : ViewModel() {
    private val mutableState = MutableStateFlow(WooPosCashManagementState())
    val state: StateFlow<WooPosCashManagementState> = mutableState.asStateFlow()
    private var historyPage = 0
    private var movementPage = 0
    private var detailRequest = 0
    private var startKey: String? = null
    private var startRequestId: UUID? = null
    private var closeKey: String? = null
    private var closeRequestId: UUID? = null
    private var adjustmentKey: String? = null
    private var adjustmentRequestId: UUID? = null

    init {
        refresh()
        loadStartPrecision()
    }

    fun loadStartPrecision() {
        viewModelScope.launch {
            try {
                val precision = currencyFormatting().numberOfDecimals
                mutableState.value = mutableState.value.copy(startPrecision = precision, startPrecisionError = null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    startPrecisionError = errorMessage(error, R.string.woopos_cash_error_currency_settings)
                )
            }
        }
    }

    fun refresh() {
        if (mutableState.value.loadingCurrent) return
        mutableState.value = mutableState.value.copy(loadingCurrent = true, currentError = null)
        viewModelScope.launch {
            try {
                val current = repository.current()
                mutableState.value = mutableState.value.copy(current = current, loadingCurrent = false)
                if (current != null && recorder.hasPending(current.id)) {
                    launch {
                        recorder.retryPending()
                        runCatching { repository.current() }.getOrNull()?.let { fresh ->
                            if (mutableState.value.current?.id == fresh.id) {
                                mutableState.value = mutableState.value.copy(current = fresh)
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loadingCurrent = false, currentError = errorMessage(error, R.string.woopos_cash_error_current)
                )
            }
        }
    }

    fun loadHistory(force: Boolean = false) {
        if (mutableState.value.loadingHistory || (historyPage > 0 && !force)) return
        mutableState.value = mutableState.value.copy(loadingHistory = true, historyError = null)
        viewModelScope.launch {
            try {
                val page = repository.past(1)
                historyPage = 1
                mutableState.value = mutableState.value.copy(
                    history = page.items, hasMoreHistory = page.hasMore, loadingHistory = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loadingHistory = false, historyError = errorMessage(error, R.string.woopos_cash_error_history)
                )
            }
        }
    }

    fun loadMoreHistory() {
        if (mutableState.value.loadingHistory || !mutableState.value.hasMoreHistory) return
        mutableState.value = mutableState.value.copy(loadingHistory = true, historyError = null)
        viewModelScope.launch {
            try {
                val page = repository.past(historyPage + 1)
                historyPage++
                val known = mutableState.value.history.map { it.id }.toSet()
                mutableState.value = mutableState.value.copy(
                    history = mutableState.value.history + page.items.filterNot { it.id in known },
                    hasMoreHistory = page.hasMore, loadingHistory = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loadingHistory = false, historyError = errorMessage(error, R.string.woopos_cash_error_history_more)
                )
            }
        }
    }

    fun selectSession(id: Long, refresh: Boolean = false) {
        val request = ++detailRequest
        val retainedDetail = mutableState.value.detail.takeIf { refresh && it?.id == id }
        val retainedMovements = if (retainedDetail != null) mutableState.value.movements else emptyList()
        mutableState.value = mutableState.value.copy(
            detailId = id, detail = retainedDetail, movements = retainedMovements,
            loadingDetail = true, loadingMovements = false, hasMoreMovements = false, detailError = null
        )
        movementPage = 0
        viewModelScope.launch {
            try {
                val detail = repository.detail(id)
                if (request != detailRequest) return@launch
                mutableState.value = mutableState.value.copy(detail = detail, loadingDetail = false, loadingMovements = true)
                val first = repository.movements(id, 1)
                if (request != detailRequest) return@launch
                movementPage = 1
                mutableState.value = mutableState.value.copy(
                    movements = first.items, hasMoreMovements = first.hasMore, loadingMovements = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == detailRequest) {
                    mutableState.value = mutableState.value.copy(
                        loadingDetail = false, loadingMovements = false,
                        detailError = errorMessage(error, if (mutableState.value.detail == null)
                            R.string.woopos_cash_error_detail else R.string.woopos_cash_error_activity)
                    )
                }
            }
        }
    }

    fun refreshDetail() { mutableState.value.detailId?.let { selectSession(it, refresh = true) } }

    fun clearDetail() {
        detailRequest++
        mutableState.value = mutableState.value.copy(detailId = null, detail = null, movements = emptyList())
    }

    fun loadMoreMovements() {
        val id = mutableState.value.detailId ?: return
        if (mutableState.value.loadingMovements || !mutableState.value.hasMoreMovements) return
        mutableState.value = mutableState.value.copy(loadingMovements = true, detailError = null)
        viewModelScope.launch {
            try {
                val page = repository.movements(id, movementPage + 1)
                if (mutableState.value.detailId != id) return@launch
                movementPage++
                val known = mutableState.value.movements.map { it.id }.toSet()
                mutableState.value = mutableState.value.copy(
                    movements = mutableState.value.movements + page.items.filterNot { it.id in known },
                    hasMoreMovements = page.hasMore, loadingMovements = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    loadingMovements = false, detailError = errorMessage(error, R.string.woopos_cash_error_activity_more)
                )
            }
        }
    }

    fun start(amount: BigDecimal, drawerName: String?, onSuccess: (Long) -> Unit) {
        val precision = mutableState.value.startPrecision ?: return
        if (mutableState.value.saving || amount < BigDecimal.ZERO || !cashAmountFitsPrecision(amount, precision)) return
        val key = "${amount.toPlainString()}:${drawerName.orEmpty()}"
        if (startKey != key) {
            startKey = key
            startRequestId = UUID.randomUUID()
        }
        save(R.string.woopos_cash_error_start) {
            val session = repository.start(amount, drawerName, requireNotNull(startRequestId))
            startKey = null
            startRequestId = null
            mutableState.value = mutableState.value.copy(current = session)
            onSuccess(session.id)
        }
    }

    fun adjust(type: String, amount: BigDecimal, reason: String, onSuccess: (Long, String?, Int) -> Unit) {
        val session = mutableState.value.current ?: return
        if (mutableState.value.saving || amount <= BigDecimal.ZERO ||
            (type == "paid_out" && amount > session.expectedCash)) return
        val key = "${session.id}:$type:${amount.toPlainString()}:${reason.trim()}"
        if (adjustmentKey != key) {
            adjustmentKey = key
            adjustmentRequestId = UUID.randomUUID()
        }
        save(R.string.woopos_cash_error_adjust) {
            val siteLocalId = repository.selectedSiteLocalId
            try {
                repository.adjust(
                    session.id, type, amount, reason.trim().ifBlank { if (type == "paid_in") "Paid in" else "Paid out" },
                    requireNotNull(adjustmentRequestId), siteLocalId
                )
            } catch (error: CashSessionException) {
                if (error.code == "woocommerce_rest_cash_insufficient_cash") refresh()
                throw error
            }
            adjustmentKey = null
            adjustmentRequestId = null
            onSuccess(session.id, session.drawerId, siteLocalId)
            refresh()
        }
    }

    fun close(counted: BigDecimal, note: String?, onSuccess: () -> Unit) {
        val session = mutableState.value.current ?: return
        if (mutableState.value.saving || counted < BigDecimal.ZERO) return
        if (recorder.hasPending(session.id)) {
            mutableState.value = mutableState.value.copy(
                operationError = resources.getString(R.string.woopos_cash_error_pending)
            )
            return
        }
        val key = "${session.id}:${session.revision}:${counted.toPlainString()}:${note.orEmpty().trim()}"
        if (closeKey != key) {
            closeKey = key
            closeRequestId = UUID.randomUUID()
        }
        save(R.string.woopos_cash_error_close) {
            try {
                val closed = repository.close(
                    session.id, session.revision, counted, note?.trim(), requireNotNull(closeRequestId)
                )
                closeKey = null
                closeRequestId = null
                mutableState.value = mutableState.value.copy(current = null, detail = closed, detailId = closed.id)
                historyPage = 0
                loadHistory(force = true)
                selectSession(closed.id, refresh = true)
                onSuccess()
            } catch (error: CashSessionException) {
                val latest = runCatching { repository.current() }.getOrNull()
                if (latest == null) {
                    val closed = runCatching { repository.detail(session.id) }.getOrNull()
                    if (closed?.status == "closed") {
                        closeKey = null
                        closeRequestId = null
                        mutableState.value = mutableState.value.copy(
                            current = null, detail = closed, detailId = closed.id
                        )
                        historyPage = 0
                        loadHistory(force = true)
                        throw CashSessionException(
                            "The cash session is already closed", "woocommerce_rest_cash_session_closed"
                        )
                    }
                }
                if (latest?.id == session.id && latest.revision != session.revision) {
                    mutableState.value = mutableState.value.copy(current = latest, recountRequired = true)
                }
                throw error
            }
        }
    }

    suspend fun allMovements(id: Long): List<WooPosCashMovement> {
        val result = mutableListOf<WooPosCashMovement>()
        var pageNumber = 1
        do {
            val page = repository.movements(id, pageNumber)
            result += page.items
            pageNumber++
        } while (page.hasMore)
        return result.distinctBy { it.id }
    }

    fun acknowledgeRecount() {
        mutableState.value = mutableState.value.copy(recountRequired = false, operationError = null)
    }

    fun clearOperationError() {
        mutableState.value = mutableState.value.copy(operationError = null)
    }

    private fun save(fallback: Int, action: suspend () -> Unit) {
        mutableState.value = mutableState.value.copy(saving = true, operationError = null)
        viewModelScope.launch {
            try {
                action()
                mutableState.value = mutableState.value.copy(saving = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false, operationError = errorMessage(error, fallback)
                )
            }
        }
    }

    private fun errorMessage(error: Exception, fallback: Int): String {
        val message = when (error) {
            is CashSessionUnsupportedException -> R.string.woopos_cash_error_unsupported
            is CashSessionException -> when (error.code) {
                "woocommerce_rest_cash_session_already_open" -> R.string.woopos_cash_error_already_open
                "woocommerce_rest_cash_drawer_already_open" -> R.string.woopos_cash_error_drawer_already_open
                "woocommerce_rest_cash_session_closed" -> R.string.woopos_cash_error_session_closed
                "woocommerce_rest_cash_session_revision_conflict" -> R.string.woopos_cash_error_recount
                "woocommerce_rest_cash_request_in_progress" -> R.string.woopos_cash_error_processing
                "woocommerce_rest_cash_insufficient_cash" -> R.string.woopos_cash_error_insufficient_cash
                "woocommerce_rest_cash_currency_mismatch", "woocommerce_rest_cash_precision_mismatch" ->
                    R.string.woopos_cash_error_currency
                "woocommerce_rest_cash_invalid_amount", "rest_invalid_param" -> R.string.woopos_cash_error_amount
                "woocommerce_rest_cash_session_not_owner" -> R.string.woopos_cash_error_not_owner
                "rest_forbidden", "woocommerce_rest_cannot_view" -> R.string.woopos_cash_error_permission
                else -> fallback
            }
            else -> fallback
        }
        return resources.getString(message)
    }
}

data class WooPosCashManagementState(
    val startPrecision: Int? = null,
    val startPrecisionError: String? = null,
    val current: WooPosCashSession? = null,
    val loadingCurrent: Boolean = false,
    val currentError: String? = null,
    val history: List<WooPosCashSession> = emptyList(),
    val loadingHistory: Boolean = false,
    val historyError: String? = null,
    val hasMoreHistory: Boolean = false,
    val detailId: Long? = null,
    val detail: WooPosCashSession? = null,
    val movements: List<WooPosCashMovement> = emptyList(),
    val hasMoreMovements: Boolean = false,
    val loadingDetail: Boolean = false,
    val loadingMovements: Boolean = false,
    val detailError: String? = null,
    val saving: Boolean = false,
    val operationError: String? = null,
    val recountRequired: Boolean = false,
)

internal fun cashAmountFitsPrecision(amount: BigDecimal, precision: Int): Boolean = amount.scale() <= precision
