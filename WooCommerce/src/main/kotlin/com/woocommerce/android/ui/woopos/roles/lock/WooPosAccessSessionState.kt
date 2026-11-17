package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status

sealed interface WooPosAccessSessionState {
    val isLocked: Boolean get() = this is Locked
    val activeRole: WooPosLocalRole? get() = null
    val status: Status get() = Status.Idle

    data object Unprotected : WooPosAccessSessionState
    data class Unlocked(override val activeRole: WooPosLocalRole) : WooPosAccessSessionState
    data class Locked(override val status: Status) : WooPosAccessSessionState
}
