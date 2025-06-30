package com.woocommerce.android.ui.woopos.home.totals

import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

private const val KEY_TTP_PAYMENT_IN_PROGRESS = "ttp_payment_in_progress"
class TTPPaymentProgressDelegate(
    private val savedStateHandle: SavedStateHandle,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return savedStateHandle.get<Boolean>(KEY_TTP_PAYMENT_IN_PROGRESS) == true
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) {
        savedStateHandle[KEY_TTP_PAYMENT_IN_PROGRESS] = value
    }
}
