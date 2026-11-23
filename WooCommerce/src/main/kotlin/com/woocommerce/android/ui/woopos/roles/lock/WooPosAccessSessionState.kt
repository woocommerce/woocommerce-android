package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status

sealed interface WooPosAccessSessionState {
    val isLocked: Boolean get() = this is Locked
    val activeRole: WooPosLocalRole? get() = null
    val status: Status get() = Status.Idle

    fun allows(capability: WooPosCapability): Boolean = when (this) {
        Unprotected -> true
        is Unlocked -> activeRole.hasCapability(capability)
        is Locked -> false
    }

    data object Unprotected : WooPosAccessSessionState
    data class Unlocked(override val activeRole: WooPosLocalRole) : WooPosAccessSessionState
    data class Locked(override val status: Status) : WooPosAccessSessionState
}
