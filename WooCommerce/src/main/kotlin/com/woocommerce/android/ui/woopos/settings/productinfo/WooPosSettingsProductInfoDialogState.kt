package com.woocommerce.android.ui.woopos.settings.productinfo

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data object WooPosSettingsProductInfoDialogState : Parcelable {
    @IgnoredOnParcel
    val header: Int = R.string.woopos_dialog_products_info_heading

    @IgnoredOnParcel
    val primaryMessage: Int = R.string.woopos_dialog_products_info_primary_message

    @IgnoredOnParcel
    val secondaryMessage: Int = R.string.woopos_dialog_products_info_secondary_message

    @IgnoredOnParcel
    val tertiaryMessage: Int = R.string.woopos_dialog_products_info_tertiary_message

    @IgnoredOnParcel
    val primaryButton: PrimaryButton = PrimaryButton(
        label = R.string.woopos_dialog_products_info_button_label,
    )

    data class PrimaryButton(
        @StringRes val label: Int,
    )
}
