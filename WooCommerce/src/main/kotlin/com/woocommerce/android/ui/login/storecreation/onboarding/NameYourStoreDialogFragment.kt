package com.woocommerce.android.ui.login.storecreation.onboarding

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.analytics.AnalyticsTracker

class NameYourStoreDialogFragment : DialogFragment() {
    companion object {
        const val TAG: String = "NameYourStoreDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inputField = EditText(context)

        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Site title")
            .setView(inputField)
            .setPositiveButton("OK") { dialog, _ ->
                // todo handle the inputText
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
