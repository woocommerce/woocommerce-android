package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus

sealed interface WooPosAccessSessionState {
    val isLocked: Boolean get() = this is Locked || this == Exiting
    val activeRole: WooPosLocalRole? get() = null
    val status: WooPosPINEntryStatus get() = WooPosPINEntryStatus.Idle

    fun allows(capability: WooPosCapability): Boolean = when (this) {
        Unprotected -> true
        is Unlocked -> activeRole.hasCapability(capability)
        is Locked, Exiting -> false
    }

    data object Exiting : WooPosAccessSessionState {
        override val status = WooPosPINEntryStatus.Checking
    }
    data object Unprotected : WooPosAccessSessionState
    data class Unlocked(override val activeRole: WooPosLocalRole) : WooPosAccessSessionState
    data class Locked(override val status: WooPosPINEntryStatus) : WooPosAccessSessionState
}
