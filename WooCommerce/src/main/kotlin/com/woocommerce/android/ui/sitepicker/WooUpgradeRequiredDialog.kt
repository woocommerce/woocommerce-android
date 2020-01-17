package com.woocommerce.android.ui.sitepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils

class WooUpgradeRequiredDialog : androidx.fragment.app.DialogFragment() {
    companion object {
        private const val TAG = "WooUpgradeRequiredDialog"

        fun newInstance() = WooUpgradeRequiredDialog()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_version_upgrade_required, container, false)

        context?.let { ctx ->
            view.findViewById<Button>(R.id.upgrade_instructions)?.setOnClickListener {
                ChromeCustomTabUtils.launchUrl(ctx, AppUrls.WOOCOMMERCE_UPGRADE)
            }
        }
        view.findViewById<Button>(R.id.upgrade_dismiss)?.setOnClickListener { dialog?.dismiss() }

        return view
    }

    override fun onResume() {
        dialog?.window?.attributes?.let { params ->
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            dialog?.window?.attributes = params
        }
        super.onResume()
    }

    fun show(manager: androidx.fragment.app.FragmentManager) {
        val ft = manager.beginTransaction()
        val prev = manager.findFragmentByTag(TAG)
        if (prev != null) {
            ft.remove(prev)
        }
        ft.addToBackStack(null)
        ft.add(this, TAG)
        ft.commitAllowingStateLoss()
    }
}
