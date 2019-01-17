package com.woocommerce.android.ui.login

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.woocommerce.android.R
import com.woocommerce.android.util.ActivityUtils
import android.view.WindowManager

class WooUpgradeRequiredDialog : DialogFragment() {
    companion object {
        const val TAG = "WooUpgradeRequiredDialog"
        private const val URL_UPGRADE_WOOCOMMERCE = "https://docs.woocommerce.com/document/how-to-update-woocommerce/"

        fun newInstance() = WooUpgradeRequiredDialog()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_version_upgrade_required, container, false)

        context?.let { ctx ->
            view.findViewById<Button>(R.id.upgrade_instructions)?.setOnClickListener {
                ActivityUtils.openUrlExternal(ctx, URL_UPGRADE_WOOCOMMERCE)
            }
        }
        view.findViewById<Button>(R.id.upgrade_dismiss)?.setOnClickListener { dialog.dismiss() }

        return view
    }

    override fun onResume() {
        dialog.window?.attributes?.let { params ->
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog.window?.attributes = params
        }
        super.onResume()
    }
}
