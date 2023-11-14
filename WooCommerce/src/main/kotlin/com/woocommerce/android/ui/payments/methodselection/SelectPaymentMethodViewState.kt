package com.woocommerce.android.ui.payments.methodselection

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.model.UiString

sealed class SelectPaymentMethodViewState {
    object Loading : SelectPaymentMethodViewState()
    data class Success(
        val orderTotal: String,
        val rows: List<Row>,
        val learnMoreIpp: LearnMoreIpp,
    ) : SelectPaymentMethodViewState() {
        sealed class Row {
            data class Single(
                @StringRes val label: Int,
                @DrawableRes val icon: Int,
                val isEnabled: Boolean,
                val onClick: () -> Unit,
            ) : Row()

            data class Double(
                @StringRes val label: Int,
                @StringRes val description: Int,
                @DrawableRes val icon: Int,
                val isEnabled: Boolean,
                val onClick: () -> Unit,
            ) : Row()
        }

        data class LearnMoreIpp(
            val label: UiString,
            val onClick: () -> Unit,
        )
    }
}
