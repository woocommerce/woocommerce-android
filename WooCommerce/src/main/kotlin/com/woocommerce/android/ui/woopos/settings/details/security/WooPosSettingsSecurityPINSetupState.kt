package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash

sealed interface WooPosSettingsSecurityPINSetupState {
    val role: WooPosLocalRole
    val canGoBack: Boolean get() = false
    val canClose: Boolean get() = true

    sealed interface Entry : WooPosSettingsSecurityPINSetupState {
        val enteredDigits: Int
        val canEnterDigits: Boolean get() = enteredDigits < WooPosPINHash.PIN_LENGTH
    }

    data class Choose(
        override val role: WooPosLocalRole,
        override val enteredDigits: Int = 0,
        val error: Error? = null,
    ) : Entry

    data class Confirm(override val role: WooPosLocalRole, override val enteredDigits: Int = 0) : Entry {
        override val canGoBack: Boolean get() = canEnterDigits
        override val canClose: Boolean get() = canEnterDigits
    }

    data class Saving(override val role: WooPosLocalRole) : WooPosSettingsSecurityPINSetupState {
        override val canClose: Boolean get() = false
    }

    data class Saved(override val role: WooPosLocalRole) : WooPosSettingsSecurityPINSetupState

    enum class Error { MISMATCH, PIN_TAKEN, SAVE_FAILED }
}

sealed interface WooPosSettingsSecurityPINSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosSettingsSecurityPINSetupUIEvent
    data object DeleteTapped : WooPosSettingsSecurityPINSetupUIEvent
    data object BackTapped : WooPosSettingsSecurityPINSetupUIEvent
}
