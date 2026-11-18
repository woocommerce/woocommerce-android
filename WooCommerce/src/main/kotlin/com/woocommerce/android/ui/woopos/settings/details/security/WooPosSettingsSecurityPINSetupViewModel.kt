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
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Entry
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
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

/** Sets a new PIN for one role. Presentation pauses run alongside the single save and never delay protection. */
@HiltViewModel
class WooPosSettingsSecurityPINSetupViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsSecurityPINSetupState>(Choose(WooPosLocalRole.ADMIN))
    val state: StateFlow<WooPosSettingsSecurityPINSetupState> = _state.asStateFlow()
    private var entry = ""
    private var chosenPIN = ""
    private var job: Job? = null

    fun start(role: WooPosLocalRole) {
        if (role != _state.value.role) restart(role)
    }

    fun reset() = restart(_state.value.role)

    private fun restart(role: WooPosLocalRole) {
        job?.cancel()
        job = null
        entry = ""
        chosenPIN = ""
        _state.value = Choose(role)
    }

    fun onUIEvent(event: WooPosSettingsSecurityPINSetupUIEvent) {
        when (event) {
            is WooPosSettingsSecurityPINSetupUIEvent.DigitTapped -> onDigit(event.digit)
            WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped -> onDelete()
            WooPosSettingsSecurityPINSetupUIEvent.BackTapped -> if (_state.value.canGoBack) reset()
        }
    }

    private fun onDigit(digit: Int) {
        val current = _state.value as? Entry ?: return
        if (!current.canEnterDigits || digit !in MIN_DIGIT..MAX_DIGIT) return
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
        chosenPIN = pin
        delay(FILLED_DOTS_HOLD_MILLIS)
        _state.value = Confirm(role)
    }

    private suspend fun confirm(role: WooPosLocalRole, pin: String) {
        val chosen = chosenPIN
        chosenPIN = ""
        if (pin != chosen) {
            delay(FILLED_DOTS_HOLD_MILLIS)
            _state.value = Choose(role, error = Error.MISMATCH)
            return
        }
        val result = coroutineScope {
            val presentation = launch {
                delay(FILLED_DOTS_HOLD_MILLIS)
                _state.value = WooPosSettingsSecurityPINSetupState.Saving(role)
                delay(MINIMUM_SAVING_MILLIS)
            }
            save(role, pin).also { if (it != SaveResult.SAVED) presentation.cancel() }
        }
        _state.value = when (result) {
            SaveResult.SAVED -> WooPosSettingsSecurityPINSetupState.Saved(role)
            SaveResult.PIN_TAKEN -> Choose(role, error = Error.PIN_TAKEN)
            SaveResult.FAILED -> Choose(role, error = Error.SAVE_FAILED)
            SaveResult.STALE -> Choose(role)
        }
    }

    private suspend fun save(role: WooPosLocalRole, pin: String): SaveResult {
        val id = selectedSite.getOrNull()?.id ?: return SaveResult.STALE
        val pins = store.getPINs(id).getOrNull()
        currentCoroutineContext().ensureActive()
        if (!isSelected(id)) return SaveResult.STALE
        if (pins == null || role in pins || !canManagePINs()) return SaveResult.FAILED
        val saved = withContext(NonCancellable) {
            store.setPIN(id, role, pin).also {
                if (it.isSuccess && pins.isEmpty() && isSelected(id)) accessSession.unlock(pin)
            }
        }
        currentCoroutineContext().ensureActive()
        return when {
            saved.exceptionOrNull() == WooPosPINError.MatchesOtherRole -> SaveResult.PIN_TAKEN
            saved.isFailure -> SaveResult.FAILED
            !isSelected(id) -> SaveResult.STALE
            else -> SaveResult.SAVED
        }
    }

    private fun canManagePINs() = accessSession.state.value.allows(WooPosCapability.MANAGE_STORE_SETTINGS)

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id

    private enum class SaveResult { SAVED, PIN_TAKEN, FAILED, STALE }

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
        const val FILLED_DOTS_HOLD_MILLIS = 500L
        const val MINIMUM_SAVING_MILLIS = 1_000L
    }
}
