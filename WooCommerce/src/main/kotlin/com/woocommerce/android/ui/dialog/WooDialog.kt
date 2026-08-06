package com.woocommerce.android.ui.dialog

import android.app.Activity
import android.content.DialogInterface.OnClickListener
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

/**
 * Used to display discard dialog across the app.
 * Currently used in Products and Orders
 */
object WooDialog {
    // Weak ref to avoid leaking the context
    private var dialogRef: WeakReference<AlertDialog>? = null

    fun showDialog(
        activity: Activity,
        posBtnAction: (OnClickListener)? = null,
        negBtnAction: (OnClickListener)? = null,
        neutBtAction: (OnClickListener)? = null,
        @StringRes titleId: Int? = null,
        @StringRes messageId: Int? = null,
        @StringRes positiveButtonId: Int? = null,
        @StringRes negativeButtonId: Int? = null,
        @StringRes neutralButtonId: Int? = null,
        cancellable: Boolean = true,
        onDismiss: (() -> Unit)? = null
    ) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val builder = MaterialAlertDialogBuilder(activity)
            .setCancelable(cancellable)
            .setOnDismissListener { onCleared() }
            .apply {
                titleId?.let { setTitle(it) }
            }
            .apply {
                messageId?.let { setMessage(messageId) }
            }
            .apply {
                positiveButtonId?.let { setPositiveButton(it, posBtnAction) }
            }
            .apply {
                negativeButtonId?.let { setNegativeButton(negativeButtonId, negBtnAction) }
            }
            .apply {
                neutralButtonId?.let { setNeutralButton(it, neutBtAction) }
            }
            .apply {
                onDismiss?.let { setOnDismissListener { it() } }
            }

        dialogRef = WeakReference(builder.show())
    }

    fun onCleared() {
        dialogRef?.get()?.dismiss()
        dialogRef?.clear()
    }
}
