package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityState.Protected
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Shows the saved PINs, so the toggle only turns off after storage confirms that they are gone. */
@HiltViewModel
class WooPosSettingsSecurityViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val store: WooPosLocalPINStore,
    private val accessSession: WooPosAccessSession,
) : ViewModel() {
    private val _state = MutableStateFlow<WooPosSettingsSecurityState>(WooPosSettingsSecurityState.Loading)
    val state: StateFlow<WooPosSettingsSecurityState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch { accessSession.state.collect { refresh() } }
    }

    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch { read()?.let { _state.value = it } }
    }

    fun onProtectionToggled(isEnabled: Boolean) {
        val id = selectedSite.getOrNull()?.id
        if (isEnabled || id == null || job?.isActive == true || _state.value !is Protected) return
        job = viewModelScope.launch {
            val removed = withContext(NonCancellable) { store.removePIN(id, WooPosLocalRole.ADMIN) }.isSuccess
            currentCoroutineContext().ensureActive()
            if (!isSelected(id)) return@launch
            accessSession.initialize()
            val current = read() ?: return@launch
            _state.value = if (!removed && current is Protected) current.copy(removalFailed = true) else current
        }
    }

    private suspend fun read(): WooPosSettingsSecurityState? {
        val id = selectedSite.getOrNull()?.id ?: return WooPosSettingsSecurityState.Unavailable
        val pins = store.getPINs(id)
        currentCoroutineContext().ensureActive()
        if (!isSelected(id)) return null
        val saved = pins.getOrNull()
        return when {
            !accessSession.state.value.allows(WooPosCapability.MANAGE_STORE_SETTINGS) ->
                WooPosSettingsSecurityState.Restricted
            saved == null -> WooPosSettingsSecurityState.Unavailable
            saved.isEmpty() -> WooPosSettingsSecurityState.Off
            else -> Protected(hasCashierPIN = WooPosLocalRole.CASHIER in saved, removalFailed = false)
        }
    }

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id
}
