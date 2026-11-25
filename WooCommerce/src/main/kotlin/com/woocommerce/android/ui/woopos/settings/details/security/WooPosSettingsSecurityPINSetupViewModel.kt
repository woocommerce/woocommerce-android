package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
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

    @Volatile
    private var editor: Editor? = null
    private var entry = ""
    private var chosenPIN = ""
    private var job: Job? = null

    fun start(role: WooPosLocalRole, changingPIN: Boolean = false) {
        val siteId = selectedSite.getOrNull()?.id ?: return invalidate()
        if (editor?.let { it.role == role && it.changingPIN == changingPIN && it.siteId == siteId } == true) return
        restart(role)
        val createsAdmin = role == WooPosLocalRole.ADMIN && !changingPIN
        val scope = if (createsAdmin) accessSession.pinSetupScope() else accessSession.approvalScope()
        val request = Editor(role, changingPIN, siteId, scope)
        editor = request
        if (!isEditorCurrent(request)) invalidate()
    }

    fun reset() {
        restart(_state.value.role)
        editor = null
    }

    private fun restart(role: WooPosLocalRole) {
        clearEntry()
        _state.value = Choose(role)
    }

    private fun clearEntry() {
        job?.cancel()
        job = null
        entry = ""
        chosenPIN = ""
    }

    private fun invalidate() {
        editor = null
        clearEntry()
        _state.value = AccessUnavailable
    }

    fun onUIEvent(event: WooPosSettingsSecurityPINSetupUIEvent) {
        val request = editor ?: return
        if (_state.value !is Entry) return
        if (!isEditorCurrent(request)) return invalidate()
        when (event) {
            is WooPosSettingsSecurityPINSetupUIEvent.DigitTapped -> onDigit(request, event.digit)
            WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped -> onDelete()
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped -> if (_state.value.canGoBack) restart(_state.value.role)
        }
    }

    private fun onDigit(request: Editor, digit: Int) {
        val current = _state.value as? Entry ?: return
        if (!current.canEnterDigits || digit !in WooPosPINHash.PIN_DIGIT_RANGE) return
        entry += digit
        _state.value = current.withDigits(entry.length)
        if (entry.length < WooPosPINHash.PIN_LENGTH) return
        val pin = entry
        entry = ""
        job = viewModelScope.launch {
            if (current is Confirm) confirm(request, pin) else choose(request, pin)
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

    private suspend fun choose(request: Editor, pin: String) {
        val needsValidation = !request.createsAdmin
        val error = coroutineScope {
            launch { delay(FILLED_DOTS_HOLD_MILLIS) }
            if (needsValidation) validatePIN(request, pin) else null
        }
        if (!isEditorCurrent(request)) return invalidate()
        if (error != null) {
            presentRejection(request, Page.CHOOSE, error)
            return
        }
        chosenPIN = pin
        _state.value = Confirm(request.role)
    }

    private suspend fun validatePIN(request: Editor, pin: String): Error? {
        if (!isEditorCurrent(request)) return Error.SAVE_FAILED
        return when (store.validatePINUniqueness(request.siteId, request.role, pin).exceptionOrNull()) {
            null -> null
            WooPosPINError.MatchesOtherRole -> Error.PIN_TAKEN
            else -> Error.SAVE_FAILED
        }
    }

    private suspend fun confirm(request: Editor, pin: String) {
        val chosen = chosenPIN
        chosenPIN = ""
        if (!isEditorCurrent(request) || chosen.length != WooPosPINHash.PIN_LENGTH) return invalidate()
        if (pin != chosen) {
            presentRejection(request, Page.CONFIRM, Error.MISMATCH)
            return
        }
        val result = coroutineScope {
            val presentation = launch {
                delay(FILLED_DOTS_HOLD_MILLIS)
                _state.value = WooPosSettingsSecurityPINSetupState.Saving(request.role)
                delay(MINIMUM_SAVING_MILLIS)
            }
            save(request, pin).also { if (it != SaveResult.SAVED) presentation.cancel() }
        }
        val outcome = if (editor === request && isSelected(request.siteId)) result else SaveResult.STALE
        when (outcome) {
            SaveResult.SAVED -> _state.value = WooPosSettingsSecurityPINSetupState.Saved(request.role)
            SaveResult.ACCESS_UNAVAILABLE, SaveResult.STALE -> invalidate()
            SaveResult.PIN_TAKEN -> _state.value = Choose(request.role, error = Error.PIN_TAKEN)
            SaveResult.FAILED -> _state.value = Choose(request.role, error = Error.SAVE_FAILED)
        }
    }

    private suspend fun presentRejection(request: Editor, page: Page, error: Error) {
        if (!isEditorCurrent(request)) return invalidate()
        _state.value = Rejected(request.role, page)
        delay(PIN_SETUP_REJECTION_PAUSE_MILLIS)
        if (!isEditorCurrent(request)) return invalidate()
        _state.value = Choose(request.role, error = error)
    }

    private suspend fun save(request: Editor, pin: String): SaveResult {
        if (!isEditorCurrent(request)) return SaveResult.STALE
        val saved = persistPIN(request, pin)
        currentCoroutineContext().ensureActive()
        return when {
            saved.exceptionOrNull() == WooPosPINError.MatchesOtherRole -> SaveResult.PIN_TAKEN
            saved.exceptionOrNull() == WooPosPINError.StaleRequest -> SaveResult.STALE
            saved.isFailure -> SaveResult.FAILED
            !isSelected(request.siteId) -> SaveResult.STALE
            request.createsAdmin && accessSession.state.value.activeRole != WooPosLocalRole.ADMIN ->
                SaveResult.ACCESS_UNAVAILABLE
            else -> SaveResult.SAVED
        }
    }

    private suspend fun persistPIN(request: Editor, pin: String): Result<Unit> =
        coroutineScope {
            val requestJob = currentCoroutineContext()[Job]
            val canCommit = { requestJob?.isActive == true && isEditorCurrent(request) }
            withContext(NonCancellable) {
                if (request.createsAdmin) {
                    val retained = accessSession.preparePINActivation(request.siteId, canCommit)
                    if (retained.isFailure) return@withContext retained
                }
                val saved = if (request.changingPIN) {
                    store.replacePIN(request.siteId, request.role, pin, canCommit)
                } else {
                    store.createPIN(request.siteId, request.role, pin, canCommit)
                }
                saved.also {
                    if (it.isSuccess && request.createsAdmin && isSelected(request.siteId)) {
                        if (request.scope == accessSession.pinSetupScope() && canManagePINs()) {
                            accessSession.unlock(pin)
                        } else {
                            accessSession.blockForStorageFailure()
                            accessSession.initialize()
                        }
                    }
                }
            }
        }

    private fun canManagePINs() = accessSession.allows(WooPosCapability.MANAGE_STORE_SETTINGS)

    private fun isEditorCurrent(request: Editor): Boolean =
        editor === request && isSelected(request.siteId) && canManagePINs() &&
            if (request.createsAdmin) {
                accessSession.state.value == WooPosAccessSessionState.Unprotected &&
                    request.scope != null && request.scope == accessSession.pinSetupScope()
            } else {
                request.scope != null && request.scope == accessSession.approvalScope()
            }

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id

    private data class Editor(
        val role: WooPosLocalRole,
        val changingPIN: Boolean,
        val siteId: Int,
        val scope: WooPosAccessSession.ApprovalScope?,
    ) {
        val createsAdmin: Boolean get() = role == WooPosLocalRole.ADMIN && !changingPIN
    }

    private enum class SaveResult { SAVED, ACCESS_UNAVAILABLE, PIN_TAKEN, FAILED, STALE }

    companion object {
        internal const val PIN_SETUP_REJECTION_PAUSE_MILLIS = 550L
        const val FILLED_DOTS_HOLD_MILLIS = 500L
        const val MINIMUM_SAVING_MILLIS = 1_000L
    }
}
