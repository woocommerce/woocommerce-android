package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.qrcode.QrCodeLoginListener
import org.wordpress.android.login.LoginMagicLinkSentImprovedFragment

class WooLoginMagicLinkSentImprovedFragment : LoginMagicLinkSentImprovedFragment() {
    companion object {
        fun newInstance(email: String?, allowPassword: Boolean): WooLoginMagicLinkSentImprovedFragment {
            val fragment = WooLoginMagicLinkSentImprovedFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL_ADDRESS, email)
            args.putBoolean(ARG_ALLOW_PASSWORD, allowPassword)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var qrCodeLoginListener: QrCodeLoginListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is WooLoginEmailFragment.Listener) {
            qrCodeLoginListener = activity as QrCodeLoginListener
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scanQrCodeButton = view.findViewById<Button>(R.id.magic_link_sent_scan_qr_code)
        scanQrCodeButton.visibility = View.VISIBLE
        scanQrCodeButton.setOnClickListener { qrCodeLoginListener.onScanQrCodeClicked(TAG) }

        view.findViewById<Button>(R.id.login_enter_password).text =
            getString(R.string.or_use_password_below_qr_code_scan_option)
    }
}
