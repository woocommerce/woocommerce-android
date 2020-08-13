package com.woocommerce.android.ui.dialog

import android.app.Activity
import android.content.DialogInterface.OnClickListener
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.annotation.StringRes
import com.woocommerce.android.R.string
import java.lang.ref.WeakReference

/**
 * Used to display discard dialog across the app.
 * Currently used in Products and Orders
 */
object CustomDiscardDialog {
    // Weak ref to avoid leaking the context
    private var dialogRef: WeakReference<AlertDialog>? = null

    fun showDiscardDialog(
        activity: Activity,
        posBtnAction: (OnClickListener)? = null,
        negBtnAction: (OnClickListener)? = null,
        @StringRes titleId: Int? = null,
        @StringRes messageId: Int? = null,
        @StringRes positiveButtonId: Int? = null,
        @StringRes negativeButtonId: Int? = null
    ) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val message = messageId?.let {
            activity.applicationContext.getString(it)
        } ?: activity.applicationContext.getString(string.discard_message)

        val positiveButtonTextId = positiveButtonId ?: string.discard
        val negativeButtonTextId = negativeButtonId ?: string.keep_editing

        val builder = MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(positiveButtonTextId, posBtnAction)
                .setNegativeButton(negativeButtonTextId, negBtnAction)
                .setOnDismissListener { onCleared() }

        titleId?.let { builder.setTitle(activity.applicationContext.getString(it)) }

        dialogRef = WeakReference(builder.show())
    }

    fun onCleared() {
        dialogRef?.get()?.dismiss()
        dialogRef?.clear()
    }
}
