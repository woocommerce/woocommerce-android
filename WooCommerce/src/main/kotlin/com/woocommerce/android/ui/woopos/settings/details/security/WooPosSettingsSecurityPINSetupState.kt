package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import kotlinx.coroutines.delay

sealed interface WooPosSettingsSecurityPINSetupState {
    val role: WooPosLocalRole
    val canGoBack: Boolean get() = false
    val canClose: Boolean get() = true

    sealed interface Entry : WooPosSettingsSecurityPINSetupState {
        val page: Page
        val enteredDigits: Int
        val canEnterDigits: Boolean get() = enteredDigits < WooPosPINHash.PIN_LENGTH
    }

    data class Choose(
        override val role: WooPosLocalRole,
        override val enteredDigits: Int = 0,
        val error: Error? = null,
    ) : Entry {
        override val page = Page.CHOOSE
    }

    data class Confirm(override val role: WooPosLocalRole, override val enteredDigits: Int = 0) : Entry {
        override val page = Page.CONFIRM
        override val canGoBack: Boolean get() = canEnterDigits
        override val canClose: Boolean get() = canEnterDigits
    }

    data class Rejected(override val role: WooPosLocalRole, override val page: Page) : Entry {
        override val enteredDigits = WooPosPINHash.PIN_LENGTH
        override val canClose: Boolean get() = page == Page.CHOOSE
    }

    data class Saving(override val role: WooPosLocalRole) : WooPosSettingsSecurityPINSetupState {
        override val canClose: Boolean get() = false
    }

    data class Saved(override val role: WooPosLocalRole) : WooPosSettingsSecurityPINSetupState

    data object AccessUnavailable : WooPosSettingsSecurityPINSetupState {
        override val role: WooPosLocalRole get() = WooPosLocalRole.ADMIN
        override val canClose: Boolean get() = false
    }

    enum class Page { CHOOSE, CONFIRM }

    enum class Error { MISMATCH, PIN_TAKEN, SAVE_FAILED }
}

internal suspend fun presentPINSetupRejection(
    role: WooPosLocalRole,
    page: WooPosSettingsSecurityPINSetupState.Page,
    error: WooPosSettingsSecurityPINSetupState.Error,
    show: (WooPosSettingsSecurityPINSetupState.Entry) -> Unit,
) {
    show(WooPosSettingsSecurityPINSetupState.Rejected(role, page))
    delay(PIN_SETUP_REJECTION_PAUSE_MILLIS)
    show(WooPosSettingsSecurityPINSetupState.Choose(role, error = error))
}

internal const val PIN_SETUP_REJECTION_PAUSE_MILLIS = 550L

sealed interface WooPosSettingsSecurityPINSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosSettingsSecurityPINSetupUIEvent
    data object DeleteTapped : WooPosSettingsSecurityPINSetupUIEvent
    data object BackTapped : WooPosSettingsSecurityPINSetupUIEvent
}
