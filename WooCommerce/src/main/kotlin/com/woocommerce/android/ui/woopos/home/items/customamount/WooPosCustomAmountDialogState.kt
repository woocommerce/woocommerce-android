package com.woocommerce.android.ui.woopos.home.items.customamount

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import java.math.BigDecimal

@Parcelize
data class WooPosCustomAmountDialogState(
    val mode: Mode = Mode.Add,
    val amount: BigDecimal? = null,
    val name: String = "",
    val isTaxable: Boolean = false,
    val isSubmitting: Boolean = false,
    val currencySymbol: String = "",
    val currencyPosition: CurrencyPosition = CurrencyPosition.LEFT,
    val decimalSeparator: String = ".",
    val numberOfDecimals: Int = 2,
    val screen: Screen = Screen.AMOUNT,
) : Parcelable {

    val isSubmitEnabled: Boolean
        get() = amount != null && amount > BigDecimal.ZERO && !isSubmitting

    enum class Screen { AMOUNT, DETAILS }

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data object Add : Mode()

        @Parcelize
        data class Edit(
            val itemNumber: Int,
        ) : Mode()
    }
}
