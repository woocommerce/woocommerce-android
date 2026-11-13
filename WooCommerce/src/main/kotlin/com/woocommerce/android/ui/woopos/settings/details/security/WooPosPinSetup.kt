package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.common.pin.WOO_POS_PIN_LENGTH
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface WooPosPinSetup {
    val state: StateFlow<WooPosPinSetupState>
    fun onUIEvent(event: WooPosPinSetupUIEvent)
}

data class WooPosPinSetupState(
    val stage: Stage = Stage.CHOOSE,
    val enteredDigits: Int = 0,
    val mismatch: Boolean = false,
) {
    enum class Stage { CHOOSE, CONFIRM, CHECKING, SUCCESS }
    val canEnterPIN: Boolean
        get() = stage == Stage.CHOOSE || stage == Stage.CONFIRM
}

sealed interface WooPosPinSetupUIEvent {
    data class DigitTapped(val digit: Int) : WooPosPinSetupUIEvent
    data object DeleteTapped : WooPosPinSetupUIEvent
    data object BackTapped : WooPosPinSetupUIEvent
}

/** Presentation fixture only. It counts key presses and never retains or compares a PIN. */
class WooPosMockPinSetup @Inject constructor() : WooPosPinSetup {
    private val _state = MutableStateFlow(WooPosPinSetupState())
    override val state: StateFlow<WooPosPinSetupState> = _state.asStateFlow()

    override fun onUIEvent(event: WooPosPinSetupUIEvent) {
        _state.update { current ->
            when (event) {
                WooPosPinSetupUIEvent.BackTapped -> WooPosPinSetupState()
                WooPosPinSetupUIEvent.DeleteTapped -> if (current.canEnterPIN) {
                    current.copy(enteredDigits = (current.enteredDigits - 1).coerceAtLeast(0))
                } else {
                    current
                }
                is WooPosPinSetupUIEvent.DigitTapped -> when {
                    event.digit !in 0..MAX_DIGIT || !current.canEnterPIN -> current
                    current.enteredDigits < WOO_POS_PIN_LENGTH - 1 -> current.copy(
                        enteredDigits = current.enteredDigits + 1,
                        mismatch = false,
                    )
                    current.stage == WooPosPinSetupState.Stage.CHOOSE -> WooPosPinSetupState(
                        stage = WooPosPinSetupState.Stage.CONFIRM
                    )
                    else -> WooPosPinSetupState(
                        stage = WooPosPinSetupState.Stage.CHECKING,
                        enteredDigits = WOO_POS_PIN_LENGTH,
                    )
                }
            }
        }
    }

    fun show(state: WooPosPinSetupState) {
        _state.value = state
    }

    private companion object {
        const val MAX_DIGIT = 9
    }
}
