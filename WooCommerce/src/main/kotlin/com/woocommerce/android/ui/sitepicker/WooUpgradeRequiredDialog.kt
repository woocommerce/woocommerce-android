package com.woocommerce.android.ui.sitepicker

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils

class WooUpgradeRequiredDialog : DialogFragment() {
    companion object {
        const val TAG = "WooUpgradeRequiredDialog"

        fun show(): WooUpgradeRequiredDialog {
            return WooUpgradeRequiredDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = View.inflate(activity, R.layout.dialog_version_upgrade_required, null)

        dialogView.findViewById<Button>(R.id.upgrade_instructions)?.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.WOOCOMMERCE_UPGRADE)
        }
        dialogView.findViewById<Button>(R.id.upgrade_dismiss)?.setOnClickListener { dialog?.dismiss() }

        return MaterialAlertDialogBuilder(activity as Context)
            .setView(dialogView)
            .setCancelable(false)
            .create()
    }

    override fun onResume() {
        dialog?.window?.attributes?.let { params ->
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog?.window?.attributes = params
        }
        super.onResume()
    }
}
