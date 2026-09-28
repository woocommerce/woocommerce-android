package com.woocommerce.android.ui.woopos.cashmanagement

import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerController
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

    init { refresh() }

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
                    loadingCurrent = false, currentError = error.message ?: "Could not load cash session"
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
                    loadingHistory = false, historyError = error.message ?: "Could not load past sessions"
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
                    loadingHistory = false, historyError = error.message ?: "Could not load more sessions"
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
                val first = repository.movements(id, 1)
                if (request != detailRequest) return@launch
                movementPage = 1
                mutableState.value = mutableState.value.copy(
                    detail = detail, movements = first.items, hasMoreMovements = first.hasMore, loadingDetail = false
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (request == detailRequest) {
                    mutableState.value = mutableState.value.copy(
                        loadingDetail = false, detailError = error.message ?: "Could not load session details"
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
                    loadingMovements = false, detailError = error.message ?: "Could not load more activity"
                )
            }
        }
    }

    fun start(amount: BigDecimal, drawerName: String?, onSuccess: (Long) -> Unit) {
        if (mutableState.value.saving || amount < BigDecimal.ZERO) return
        val key = "${amount.toPlainString()}:${drawerName.orEmpty()}"
        if (startKey != key) {
            startKey = key
            startRequestId = UUID.randomUUID()
        }
        save {
            val session = repository.start(amount, drawerName, requireNotNull(startRequestId))
            startKey = null
            startRequestId = null
            mutableState.value = mutableState.value.copy(current = session)
            onSuccess(session.id)
        }
    }

    fun adjust(type: String, amount: BigDecimal, reason: String, onSuccess: (Long, String?) -> Unit) {
        val session = mutableState.value.current ?: return
        if (mutableState.value.saving || amount <= BigDecimal.ZERO ||
            (type == "pay_out" && amount > session.expectedCash)) return
        val key = "${session.id}:$type:${amount.toPlainString()}:${reason.trim()}"
        if (adjustmentKey != key) {
            adjustmentKey = key
            adjustmentRequestId = UUID.randomUUID()
        }
        save {
            repository.adjust(
                session.id, type, amount, reason.trim().ifBlank { if (type == "pay_in") "Paid in" else "Paid out" },
                requireNotNull(adjustmentRequestId)
            )
            adjustmentKey = null
            adjustmentRequestId = null
            onSuccess(session.id, session.drawerId)
            refresh()
        }
    }

    fun close(counted: BigDecimal, note: String?, onSuccess: () -> Unit) {
        val session = mutableState.value.current ?: return
        if (mutableState.value.saving || counted < BigDecimal.ZERO) return
        if (recorder.hasPending(session.id)) {
            mutableState.value = mutableState.value.copy(
                operationError = "Cash movements are still waiting to sync. Retry before closing."
            )
            return
        }
        val key = "${session.id}:${session.revision}:${counted.toPlainString()}:${note.orEmpty().trim()}"
        if (closeKey != key) {
            closeKey = key
            closeRequestId = UUID.randomUUID()
        }
        save {
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
                        mutableState.value = mutableState.value.copy(current = null)
                        historyPage = 0
                        loadHistory(force = true)
                        selectSession(closed.id, refresh = true)
                        onSuccess()
                        return@save
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

    private fun save(action: suspend () -> Unit) {
        mutableState.value = mutableState.value.copy(saving = true, operationError = null)
        viewModelScope.launch {
            try {
                action()
                mutableState.value = mutableState.value.copy(saving = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    saving = false, operationError = error.message ?: "The cash session could not be saved"
                )
            }
        }
    }
}

data class WooPosCashManagementState(
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
