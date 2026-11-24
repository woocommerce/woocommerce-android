package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINError
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.AccessUnavailable
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Entry
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Page
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Rejected
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsSecurityPINSetupViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsSecurityPINSetupState>(Choose(WooPosLocalRole.ADMIN))
    val state: StateFlow<WooPosSettingsSecurityPINSetupState> = _state.asStateFlow()
    private var editor: Editor? = null
    private val changingPIN: Boolean get() = editor?.changingPIN == true
    private var entry = ""
    private var chosenPIN = ""
    private var chosenSiteId: Int? = null
    private var job: Job? = null

    fun start(role: WooPosLocalRole, changingPIN: Boolean = false) {
        if (editor?.let { it.role == role && it.changingPIN == changingPIN } == true) return
        restart(role)
        editor = Editor(role, changingPIN, accessSession.approvalScope())
    }

    fun reset() {
        restart(_state.value.role)
        editor = null
    }

    private fun restart(role: WooPosLocalRole) {
        job?.cancel()
        job = null
        entry = ""
        chosenPIN = ""
        chosenSiteId = null
        _state.value = Choose(role)
    }

    fun onUIEvent(event: WooPosSettingsSecurityPINSetupUIEvent) {
        when (event) {
            is WooPosSettingsSecurityPINSetupUIEvent.DigitTapped -> onDigit(event.digit)
            WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped -> onDelete()
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped -> if (_state.value.canGoBack) restart(_state.value.role)
        }
    }

    private fun onDigit(digit: Int) {
        val current = _state.value as? Entry ?: return
        if (!current.canEnterDigits || digit !in WooPosPINHash.PIN_DIGIT_RANGE) return
        entry += digit
        _state.value = current.withDigits(entry.length)
        if (entry.length < WooPosPINHash.PIN_LENGTH) return
        val pin = entry
        entry = ""
        job = viewModelScope.launch {
            if (current is Confirm) confirm(current.role, pin) else choose(current.role, pin)
        }
    }

    private fun onDelete() {
        val current = _state.value as? Entry ?: return
        if (!current.canEnterDigits || entry.isEmpty()) return
        entry = entry.dropLast(1)
        _state.value = current.withDigits(entry.length)
    }

    private fun Entry.withDigits(digits: Int): Entry =
        if (this is Confirm) Confirm(role, digits) else Choose(role, digits)

    private suspend fun choose(role: WooPosLocalRole, pin: String) {
        val siteId = selectedSite.getOrNull()?.id
        val needsValidation = role == WooPosLocalRole.CASHIER || changingPIN
        val error = coroutineScope {
            launch { delay(FILLED_DOTS_HOLD_MILLIS) }
            if (needsValidation && siteId != null) validatePIN(siteId, role, pin) else null
        }
        if (needsValidation) {
            if (siteId == null || !isSelected(siteId)) return reset()
            val currentError = if (canManagePINs()) error else Error.SAVE_FAILED
            if (currentError != null) {
                presentRejection(role, siteId, Page.CHOOSE, currentError)
                return
            }
        }
        chosenPIN = pin
        chosenSiteId = siteId
        _state.value = Confirm(role)
    }

    private suspend fun validatePIN(siteId: Int, role: WooPosLocalRole, pin: String): Error? {
        if (!canManagePINs()) return Error.SAVE_FAILED
        return when (store.validatePINUniqueness(siteId, role, pin).exceptionOrNull()) {
            null -> null
            WooPosPINError.MatchesOtherRole -> Error.PIN_TAKEN
            else -> Error.SAVE_FAILED
        }
    }

    private suspend fun confirm(role: WooPosLocalRole, pin: String) {
        val chosen = chosenPIN
        val siteId = chosenSiteId ?: return reset()
        chosenPIN = ""
        chosenSiteId = null
        if (pin != chosen) {
            presentRejection(role, siteId, Page.CONFIRM, Error.MISMATCH)
            return
        }
        val result = coroutineScope {
            val presentation = launch {
                delay(FILLED_DOTS_HOLD_MILLIS)
                _state.value = WooPosSettingsSecurityPINSetupState.Saving(role)
                delay(MINIMUM_SAVING_MILLIS)
            }
            save(role, siteId, pin).also { if (it != SaveResult.SAVED) presentation.cancel() }
        }
        val outcome = if (isSelected(siteId)) result else SaveResult.STALE
        _state.value = when (outcome) {
            SaveResult.SAVED -> WooPosSettingsSecurityPINSetupState.Saved(role)
            SaveResult.ACCESS_UNAVAILABLE -> AccessUnavailable
            SaveResult.PIN_TAKEN -> Choose(role, error = Error.PIN_TAKEN)
            SaveResult.FAILED -> Choose(role, error = Error.SAVE_FAILED)
            SaveResult.STALE -> Choose(role)
        }
    }

    private suspend fun presentRejection(role: WooPosLocalRole, siteId: Int, page: Page, error: Error) {
        if (!isSelected(siteId)) return reset()
        _state.value = Rejected(role, page)
        delay(PIN_SETUP_REJECTION_PAUSE_MILLIS)
        val visibleError = when {
            !isSelected(siteId) -> null
            page == Page.CHOOSE && !canManagePINs() -> Error.SAVE_FAILED
            else -> error
        }
        _state.value = Choose(role, error = visibleError)
    }

    private suspend fun save(role: WooPosLocalRole, id: Int, pin: String): SaveResult {
        if (!isSelected(id)) return SaveResult.STALE
        val activatesAdmin = role == WooPosLocalRole.ADMIN && !changingPIN
        val canCreate = !activatesAdmin || store.getPINs(id).getOrNull()?.isEmpty() == true
        currentCoroutineContext().ensureActive()
        if (!isSelected(id)) return SaveResult.STALE
        if (!canCreate || !canManagePINs() || !isEditorCurrent()) return SaveResult.FAILED
        val saved = persistPIN(id, role, pin, activatesAdmin)
        currentCoroutineContext().ensureActive()
        return when {
            saved.exceptionOrNull() == WooPosPINError.MatchesOtherRole -> SaveResult.PIN_TAKEN
            saved.isFailure -> SaveResult.FAILED
            !isSelected(id) -> SaveResult.STALE
            activatesAdmin && accessSession.state.value.activeRole != WooPosLocalRole.ADMIN ->
                SaveResult.ACCESS_UNAVAILABLE
            else -> SaveResult.SAVED
        }
    }

    private suspend fun persistPIN(id: Int, role: WooPosLocalRole, pin: String, activatesAdmin: Boolean): Result<Unit> =
        coroutineScope {
            val requestJob = currentCoroutineContext()[Job]
            val canCommit = { requestJob?.isActive == true && isSelected(id) && isEditorCurrent() && canManagePINs() }
            withContext(NonCancellable) {
                val saved = if (changingPIN) {
                    store.setPIN(id, role, pin, canCommit)
                } else {
                    store.createPIN(id, role, pin, canCommit)
                }
                saved.also {
                    if (it.isSuccess && activatesAdmin && isSelected(id)) accessSession.unlock(pin)
                }
            }
        }

    private fun canManagePINs() = accessSession.allows(WooPosCapability.MANAGE_STORE_SETTINGS)

    private fun isEditorCurrent(): Boolean {
        val scope = editor?.scope
        return (scope != null || !changingPIN) && (scope == null || scope == accessSession.approvalScope())
    }

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id

    private data class Editor(
        val role: WooPosLocalRole,
        val changingPIN: Boolean,
        val scope: WooPosAccessSession.ApprovalScope?,
    )

    private enum class SaveResult { SAVED, ACCESS_UNAVAILABLE, PIN_TAKEN, FAILED, STALE }

    companion object {
        internal const val PIN_SETUP_REJECTION_PAUSE_MILLIS = 550L
        const val FILLED_DOTS_HOLD_MILLIS = 500L
        const val MINIMUM_SAVING_MILLIS = 1_000L
    }
}
