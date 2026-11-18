package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The session reports Unprotected only after it reads storage without PINs, so unreadable storage never offers setup. */
@HiltViewModel
class WooPosSettingsSecurityViewModel @Inject constructor(
    accessSession: WooPosAccessSession,
) : ViewModel() {
    val isProtectionEnabled: StateFlow<Boolean> = accessSession.state
        .map(::isProtected)
        .stateIn(viewModelScope, SharingStarted.Eagerly, isProtected(accessSession.state.value))

    private fun isProtected(state: WooPosAccessSessionState) = state != WooPosAccessSessionState.Unprotected
}
