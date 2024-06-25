package com.woocommerce.android.ui.woopos.common.composeui.component.snackbar

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class WooPosSnackbarState : Parcelable {
    data class Triggered(@StringRes val message: Int) : WooPosSnackbarState()
    data object Hidden : WooPosSnackbarState()
}
