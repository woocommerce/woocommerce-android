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
    private var readJob: Job? = null
    private var removalJob: Job? = null

    init {
        viewModelScope.launch { accessSession.state.collect { refresh() } }
    }

    fun refresh() {
        readJob?.cancel()
        readJob = viewModelScope.launch { read()?.let { _state.value = it } }
    }

    suspend fun removeProtection(): Boolean = removePIN(WooPosLocalRole.ADMIN)

    fun deleteCashierPIN() {
        if (removalJob?.isActive == true) return
        removalJob = viewModelScope.launch { removePIN(WooPosLocalRole.CASHIER) }
    }

    private suspend fun removePIN(role: WooPosLocalRole): Boolean {
        val scope = accessSession.approvalScope() ?: return false
        val requestJob = currentCoroutineContext()[Job] ?: return false
        if (!canRemovePINs(requestJob)) return false
        removalJob = requestJob
        readJob?.cancel()
        val removed = persistRemoval(role, scope, requestJob)
        currentCoroutineContext().ensureActive()
        read()?.let { current ->
            _state.value = if (!removed && isSelected(scope.siteId) && current is Protected) {
                current.copy(removalFailed = true)
            } else {
                current
            }
        }
        return removed && isSelected(scope.siteId)
    }

    private fun canRemovePINs(requestJob: Job): Boolean {
        val anotherRemovalPending = removalJob?.isActive == true && removalJob !== requestJob
        return _state.value is Protected && canManagePINs() && !anotherRemovalPending
    }

    private suspend fun persistRemoval(
        role: WooPosLocalRole,
        scope: WooPosAccessSession.ApprovalScope,
        requestJob: Job,
    ): Boolean = withContext(NonCancellable) {
        store.removePIN(scope.siteId, role) {
            requestJob.isActive && accessSession.approvalScope() == scope && canManagePINs()
        }.also { if (it.isSuccess && isSelected(scope.siteId)) accessSession.initialize() }.isSuccess
    }

    private suspend fun read(): WooPosSettingsSecurityState? {
        if (!canManagePINs()) return WooPosSettingsSecurityState.Restricted
        val id = selectedSite.getOrNull()?.id ?: return WooPosSettingsSecurityState.Unavailable
        val pins = store.getPINs(id)
        currentCoroutineContext().ensureActive()
        if (!isSelected(id)) return null
        val saved = pins.getOrNull()
        return when {
            !canManagePINs() -> WooPosSettingsSecurityState.Restricted
            saved == null -> WooPosSettingsSecurityState.Unavailable
            saved.isEmpty() -> WooPosSettingsSecurityState.Off
            else -> Protected(hasCashierPIN = WooPosLocalRole.CASHIER in saved, removalFailed = false)
        }
    }

    private fun canManagePINs() = accessSession.allows(WooPosCapability.MANAGE_STORE_SETTINGS)

    private fun isSelected(localSiteId: Int) = localSiteId == selectedSite.getOrNull()?.id
}
