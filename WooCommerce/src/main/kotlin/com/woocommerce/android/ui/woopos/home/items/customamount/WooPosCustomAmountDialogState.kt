package com.woocommerce.android.ui.woopos.home.items.customamount

import android.os.Parcelable
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
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
    val isSubmitted: Boolean = false,
    val currencySymbol: String = "",
    val currencyPosition: CurrencyPosition = CurrencyPosition.LEFT,
    val decimalSeparator: String = ".",
    val numberOfDecimals: Int = 2,
    val screen: Screen = Screen.AMOUNT,
) : Parcelable {

    val isSubmitEnabled: Boolean
        get() = amount != null && amount > BigDecimal.ZERO && !isSubmitting && !isSubmitted

    enum class Screen { AMOUNT, FORM }

    @Parcelize
    sealed class Mode : Parcelable {
        @Parcelize
        data object Add : Mode()

        @Parcelize
        data class Edit(
            val itemNumber: Int,
            val amount: BigDecimal,
            val name: String,
            val isTaxable: Boolean,
        ) : Mode()
    }
}

fun WooPosCartItemViewState.CustomAmount.toEditMode() = WooPosCustomAmountDialogState.Mode.Edit(
    itemNumber = itemNumber,
    amount = amount,
    name = name,
    isTaxable = isTaxable,
)
