package com.woocommerce.android.ui.login

import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import org.wordpress.android.login.LoginListener

class LoginSiteInfoFallbackDialogFragment : DialogFragment() {
    override fun onStart() {
        super.onStart()
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val siteAddress = requireArguments().getString(ARG_SITE_ADDRESS).orEmpty()
        return MaterialAlertDialogBuilder(ContextThemeWrapper(requireActivity(), style.Theme_Woo_Dialog))
            .setTitle(R.string.login_site_info_fallback_dialog_title)
            .setMessage(R.string.login_site_info_fallback_dialog_message)
            .setPositiveButton(R.string.login_site_info_fallback_dialog_cta) { dialog, _ ->
                (requireActivity() as LoginListener).loginViaSiteCredentials(siteAddress)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
    }

    companion object {
        const val TAG = "LoginSiteInfoFallbackDialogFragment"
        private const val ARG_SITE_ADDRESS = "site_address"

        fun newInstance(siteAddress: String) = LoginSiteInfoFallbackDialogFragment().apply {
            arguments = bundleOf(ARG_SITE_ADDRESS to siteAddress)
        }
    }
}
