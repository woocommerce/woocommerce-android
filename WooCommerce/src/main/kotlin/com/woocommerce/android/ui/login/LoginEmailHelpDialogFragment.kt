package com.woocommerce.android.ui.login

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import dagger.android.support.AndroidSupportInjection
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject

class LoginEmailHelpDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "LoginEmailHelpDialogFragment"
    }

    interface Listener {
        fun onEmailNeedMoreHelpClicked()
    }

    private var listener: Listener? = null
    @Inject lateinit var analyticsListener: LoginAnalyticsListener

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = Html.fromHtml(getString(R.string.login_email_help_desc, "<b>", "</b>", "<b>", "</b>"))

        return MaterialAlertDialogBuilder(ContextThemeWrapper(requireActivity(), style.Theme_Woo_Dialog))
                .setTitle(R.string.login_email_help_title)
                .setMessage(message)
                .setNeutralButton(R.string.login_site_address_more_help) { dialog, _ ->
                    AnalyticsTracker.track(Stat.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_NEED_MORE_HELP_LINK_TAPPED)
                    analyticsListener.trackDismissDialog()
                    listener?.onEmailNeedMoreHelpClicked()
                    dialog.dismiss()
                }
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    AnalyticsTracker.track(Stat.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_OK_BUTTON_TAPPED)
                    analyticsListener.trackDismissDialog()
                    dialog.dismiss()
                }
                .setCancelable(true)
                .create()
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)

        if (context is Listener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.track(Stat.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_VIEWED)
    }
}
