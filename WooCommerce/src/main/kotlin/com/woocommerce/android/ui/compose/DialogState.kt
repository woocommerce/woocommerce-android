package com.woocommerce.android.ui.compose

import androidx.annotation.StringRes
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.component.AlertDialog
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.component.getText

data class DialogState(
    val title: UiString? = null,
    val message: UiString? = null,
    val positiveButton: DialogButton? = null,
    val negativeButton: DialogButton? = null,
    val neutralButton: DialogButton? = null,
    val isCancelable: Boolean = true,
    val onDismiss: () -> Unit = {}
) {
    constructor(
        @StringRes title: Int? = null,
        @StringRes message: Int? = null,
        positiveButton: DialogButton? = null,
        negativeButton: DialogButton? = null,
        neutralButton: DialogButton? = null,
        isCancelable: Boolean = true,
        onDismiss: () -> Unit = {}
    ) : this(
        title?.let { UiString.UiStringRes(it) },
        message?.let { UiString.UiStringRes(it) },
        positiveButton,
        negativeButton,
        neutralButton,
        isCancelable,
        onDismiss
    )

    data class DialogButton(
        val text: UiString,
        val onClick: () -> Unit
    ) {
        constructor(
            @StringRes text: Int,
            onClick: () -> Unit
        ) : this(UiString.UiStringRes(text), onClick)
    }
}

@Composable
fun DialogState.Render() {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = title?.let {
            {
                Text(text = title.getText())
            }
        },
        text = message?.let {
            {
                Text(text = message.getText())
            }
        },
        confirmButton = positiveButton?.let {
            {
                WCTextButton(text = it.text.getText(), onClick = it.onClick)
            }
        } ?: { },
        dismissButton = negativeButton?.let {
            {
                WCTextButton(text = it.text.getText(), onClick = it.onClick)
            }
        } ?: { },
        neutralButton = neutralButton?.let {
            {
                WCTextButton(text = it.text.getText(), onClick = it.onClick)
            }
        } ?: { },
        properties = DialogProperties(
            dismissOnBackPress = isCancelable,
            dismissOnClickOutside = isCancelable
        )
    )
}
