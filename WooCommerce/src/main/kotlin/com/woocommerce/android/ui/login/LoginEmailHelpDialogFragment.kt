package com.woocommerce.android.ui.login

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.R.style

class LoginEmailHelpDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "LoginEmailHelpDialogFragment"
    }

    interface Listener {
        fun onEmailNeedMoreHelpClicked()
    }

    private var listener: Listener? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Dialog_Login_EmailHelp
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = Html.fromHtml(getString(R.string.login_email_help_desc, "<b>", "</b>", "<b>", "</b>"))

        return AlertDialog.Builder(ContextThemeWrapper(activity, style.Woo_Dialog))
                .setTitle(R.string.login_email_help_title)
                .setMessage(message)
                .setNeutralButton(R.string.login_site_address_more_help) { dialog, _ ->
                    listener?.onEmailNeedMoreHelpClicked()
                    dialog.dismiss()
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .create()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is Listener) {
            listener = context
        }
    }
}
