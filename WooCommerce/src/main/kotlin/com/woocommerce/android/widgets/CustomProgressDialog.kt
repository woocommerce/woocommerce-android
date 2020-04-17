package com.woocommerce.android.widgets

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

/**
 * Used to display a custom progress dialog. The progress dialog is deprecated in Android so adding a custom
 * class temporarily when updating a product.
 */
class CustomProgressDialog : DialogFragment() {
    companion object {
        const val TAG: String = "CustomProgressDialog"

        fun show(title: String, message: String): CustomProgressDialog {
            val fragment = CustomProgressDialog()
            fragment.progressTitle = title
            fragment.progressMessage = message
            return fragment
        }
    }

    private var progressTitle: String? = null
    private var progressMessage: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = View.inflate(activity, R.layout.view_progress_dialog, null)

        dialogView.findViewById<MaterialTextView>(R.id.progress_title).text = progressTitle
        dialogView.findViewById<MaterialTextView>(R.id.progress_text).text = progressMessage

        return MaterialAlertDialogBuilder(activity as Context)
                .setView(dialogView)
                .setCancelable(false)
                .create()
    }
}
