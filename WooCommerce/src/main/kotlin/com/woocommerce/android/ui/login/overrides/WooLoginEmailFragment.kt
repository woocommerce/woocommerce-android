package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import org.wordpress.android.login.LoginEmailFragment

class WooLoginEmailFragment : LoginEmailFragment() {
    private var qrLoginListener: QrLoginListener? = null

    interface QrLoginListener {
        fun onQrCodeLoginClicked()
    }

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_screen

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        rootView.findViewById<Button>(R.id.button_login_qr_code).setOnClickListener {
            qrLoginListener?.onQrCodeLoginClicked()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (activity is QrLoginListener) {
            qrLoginListener = activity as QrLoginListener
        }
    }
}
