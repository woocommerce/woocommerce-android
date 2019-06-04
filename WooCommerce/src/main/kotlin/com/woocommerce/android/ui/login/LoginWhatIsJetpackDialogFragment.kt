package com.woocommerce.android.ui.login

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker

class LoginWhatIsJetpackDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "LoginWhatIsJetpackDialogFragment"
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Dialog_Login_WhatIsJetpack
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = View.inflate(activity, R.layout.fragment_login_what_is_jetpack, null)

        dialogView.findViewById<Button>(R.id.btn_learn_more)?.setOnClickListener {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_LEARN_MORE_BUTTON_TAPPED
            )
            // TODO: Open the help docs
            Toast.makeText(activity, "Not yet implemented", Toast.LENGTH_LONG).show()
        }

        dialogView.findViewById<Button>(R.id.btn_ok)?.setOnClickListener {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_OK_BUTTON_TAPPED)

            dialog?.dismiss()
        }

        // TODO: Add tracks events

        return AlertDialog.Builder(ContextThemeWrapper(activity, R.style.Woo_Dialog))
                .setView(dialogView)
                .setCancelable(true)
                .create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
