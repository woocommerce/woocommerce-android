package com.woocommerce.android.ui.login

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat

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
            AnalyticsTracker.track(Stat.LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_LEARN_MORE_BUTTON_TAPPED)

            ChromeCustomTabUtils.launchUrl(activity as Context, getString(R.string.jetpack_view_instructions_link))
        }

        dialogView.findViewById<Button>(R.id.btn_ok)?.setOnClickListener {
            AnalyticsTracker.track(Stat.LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_OK_BUTTON_TAPPED)

            dialog?.dismiss()
        }

        return AlertDialog.Builder(ContextThemeWrapper(activity, R.style.Woo_Dialog))
                .setView(dialogView)
                .setCancelable(true)
                .create()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        AnalyticsTracker.track(Stat.LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_VIEWED)
    }
}
