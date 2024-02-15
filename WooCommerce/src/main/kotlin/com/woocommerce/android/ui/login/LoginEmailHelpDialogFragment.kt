package com.woocommerce.android.ui.login

import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import javax.inject.Inject

@AndroidEntryPoint
class LoginEmailHelpDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "LoginEmailHelpDialogFragment"

        fun newInstance(
            listener: Listener
        ): LoginEmailHelpDialogFragment {
            return LoginEmailHelpDialogFragment().apply {
                this.listener = listener
            }
        }
    }

    interface Listener {
        fun onEmailNeedMoreHelpClicked()
    }

    private var listener: Listener? = null

    @Inject lateinit var analyticsListener: LoginAnalyticsListener

    override fun onStart() {
        super.onStart()
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = HtmlCompat.fromHtml(
            getString(R.string.login_email_help_desc, "<b>", "</b>", "<b>", "</b>"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        return MaterialAlertDialogBuilder(ContextThemeWrapper(requireActivity(), style.Theme_Woo_Dialog))
            .setTitle(R.string.login_email_help_title)
            .setMessage(message)
            .setNeutralButton(R.string.login_site_address_more_help) { dialog, _ ->
                AnalyticsTracker.track(AnalyticsEvent.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_NEED_MORE_HELP_LINK_TAPPED)
                analyticsListener.trackDismissDialog()
                listener?.onEmailNeedMoreHelpClicked()
                dialog.dismiss()
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                AnalyticsTracker.track(AnalyticsEvent.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_OK_BUTTON_TAPPED)
                analyticsListener.trackDismissDialog()
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
    }

    override fun onDetach() {
        super.onDetach()

        listener = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_VIEWED)
    }
}
