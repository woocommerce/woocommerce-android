package com.woocommerce.android.ui.dialog

import android.app.Activity
import android.content.DialogInterface.OnClickListener
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        negBtnAction: (OnClickListener)? = null
    ) {
        dialogRef?.get()?.let {
            // Dialog is already present
            return
        }

        val builder = MaterialAlertDialogBuilder(activity)
                .setMessage(activity.applicationContext.getString(string.discard_message))
                .setCancelable(true)
                .setPositiveButton(string.discard, posBtnAction)
                .setNegativeButton(string.keep_editing, negBtnAction)
                .setOnDismissListener { onCleared() }

        dialogRef = WeakReference(builder.show())
    }

    fun onCleared() {
        dialogRef?.get()?.dismiss()
        dialogRef?.clear()
    }
}
