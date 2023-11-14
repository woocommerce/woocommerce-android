package com.woocommerce.android.ui.jitm

import androidx.annotation.DrawableRes
import com.woocommerce.android.model.UiString

sealed interface JitmState {
    data class Banner(
        val onPrimaryActionClicked: () -> Unit,
        val onDismissClicked: () -> Unit,
        val title: UiString,
        val description: UiString,
        val primaryActionLabel: UiString,
        val backgroundImage: LocalOrRemoteImage,
        val badgeIcon: LabelOrRemoteIcon,
    ) : JitmState {
        sealed class LocalOrRemoteImage {
            data class Local(@DrawableRes val drawableId: Int) : LocalOrRemoteImage()
            data class Remote(
                val urlLightMode: String,
                val urlDarkMode: String,
            ) : LocalOrRemoteImage()
        }

        sealed class LabelOrRemoteIcon {
            data class Label(val label: UiString) : LabelOrRemoteIcon()
            data class Remote(
                val urlLightMode: String,
                val urlDarkMode: String,
            ) : LabelOrRemoteIcon()
        }
    }

    data class Modal(
        val onPrimaryActionClicked: () -> Unit,
        val onDismissClicked: () -> Unit,
        val title: UiString,
        val description: UiString,
        val primaryActionLabel: UiString,
        val backgroundLightImageUrl: String?,
        val backgroundDarkImageUrl: String?,
    ) : JitmState

    object Hidden : JitmState
}
