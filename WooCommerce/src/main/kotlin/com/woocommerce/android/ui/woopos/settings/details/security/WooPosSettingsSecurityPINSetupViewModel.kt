package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
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

/** Sets the first Admin PIN. Presentation pauses run alongside the single save and never delay protection. */
@HiltViewModel
class WooPosSettingsSecurityPINSetupViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsSecurityPINSetupState>(Choose())
    val state: StateFlow<WooPosSettingsSecurityPINSetupState> = _state.asStateFlow()
    private var entry = ""
    private var chosenPIN = ""
    private var chosenSiteId: Int? = null
    private var job: Job? = null

    fun reset() {
        job?.cancel()
        job = null
        entry = ""
        chosenPIN = ""
        chosenSiteId = null
        _state.value = Choose()
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
        _state.value = if (current is Confirm) Confirm(entry.length) else Choose(entry.length)
        if (entry.length < WooPosPINHash.PIN_LENGTH) return
        val pin = entry
        entry = ""
        job = viewModelScope.launch { if (current is Confirm) confirm(pin) else choose(pin) }
    }

    private fun onDelete() {
        val current = _state.value as? Entry ?: return
        if (!current.canEnterDigits || entry.isEmpty()) return
        entry = entry.dropLast(1)
        _state.value = if (current is Confirm) Confirm(entry.length) else Choose(entry.length)
    }

    private suspend fun choose(pin: String) {
        chosenPIN = pin
        chosenSiteId = selectedSite.getOrNull()?.id
        delay(FILLED_DOTS_HOLD_MILLIS)
        _state.value = Confirm()
    }

    private suspend fun confirm(pin: String) {
        val chosen = chosenPIN
        val siteId = chosenSiteId ?: return reset()
        chosenPIN = ""
        chosenSiteId = null
        if (pin != chosen) {
            delay(FILLED_DOTS_HOLD_MILLIS)
            _state.value = Choose(error = Error.MISMATCH)
            return
        }
        val result = coroutineScope {
            val presentation = launch {
                delay(FILLED_DOTS_HOLD_MILLIS)
                _state.value = WooPosSettingsSecurityPINSetupState.Saving
                delay(MINIMUM_SAVING_MILLIS)
            }
            save(siteId, pin).also { if (it != SaveResult.SAVED) presentation.cancel() }
        }
        val outcome = if (isSelected(siteId)) result else SaveResult.STALE
        _state.value = when (outcome) {
            SaveResult.SAVED -> WooPosSettingsSecurityPINSetupState.Saved
            SaveResult.FAILED -> Choose(error = Error.SAVE_FAILED)
            SaveResult.STALE -> Choose()
        }
    }

    private suspend fun save(id: Int, pin: String): SaveResult {
        if (!isSelected(id)) return SaveResult.STALE
        val pins = store.getPINs(id)
        currentCoroutineContext().ensureActive()
        if (!isSelected(id)) return SaveResult.STALE
        val canCreate = pins.getOrNull()?.let { WooPosLocalRole.ADMIN !in it } == true
        if (!canCreate) return SaveResult.FAILED
        val saved = withContext(NonCancellable) {
            val pinSaved = store.setPIN(id, WooPosLocalRole.ADMIN, pin).isSuccess
            if (pinSaved && isSelected(id)) accessSession.unlock(pin)
            pinSaved
        }
        currentCoroutineContext().ensureActive()
        return when {
            !saved -> SaveResult.FAILED
            !isSelected(id) -> SaveResult.STALE
            else -> SaveResult.SAVED
        }
    }

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id

    private enum class SaveResult { SAVED, FAILED, STALE }

    companion object {
        private const val MIN_DIGIT = 0
        private const val MAX_DIGIT = 9
        const val FILLED_DOTS_HOLD_MILLIS = 500L
        const val MINIMUM_SAVING_MILLIS = 1_000L
    }
}
