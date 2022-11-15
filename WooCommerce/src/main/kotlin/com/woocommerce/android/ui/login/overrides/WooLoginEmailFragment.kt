package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.experiment.SimplifiedLoginExperiment
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant.CONTROL
import com.woocommerce.android.experiment.SimplifiedLoginExperiment.LoginVariant.SIMPLIFIED_LOGIN_WPCOM
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.ui.login.qrcode.QrCodeLoginListener
import org.wordpress.android.login.LoginEmailFragment
import org.wordpress.android.login.widgets.WPLoginInputRow
import javax.inject.Inject

class WooLoginEmailFragment : LoginEmailFragment() {
    interface Listener {
        fun onWhatIsWordPressLinkClicked()
    }

    private lateinit var wooLoginEmailListener: Listener
    private lateinit var qrCodeLoginListener: QrCodeLoginListener

    @Inject
    lateinit var simplifiedLoginExperiment: SimplifiedLoginExperiment

    @LayoutRes
    override fun getContentLayout(): Int = when (simplifiedLoginExperiment.getCurrentVariant()) {
        CONTROL -> R.layout.fragment_login_email_screen
        SIMPLIFIED_LOGIN_WPCOM -> R.layout.fragment_simplified_login_email_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        val whatIsWordPressText = rootView.findViewById<Button>(R.id.login_what_is_wordpress)
        whatIsWordPressText.setOnClickListener {
            wooLoginEmailListener.onWhatIsWordPressLinkClicked()
        }

        rootView.findViewById<Button>(R.id.button_login_qr_code).setOnClickListener {
            qrCodeLoginListener.onScanQrCodeClicked(TAG)
        }
    }

    override fun setupLabel(label: TextView) {
        // NO-OP, For this custom screen, the correct label is set in the layout
    }

    override fun onResume() {
        super.onResume()
        requireView().findViewById<WPLoginInputRow>(R.id.login_email_row).editText.showKeyboardWithDelay(0)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            wooLoginEmailListener = activity as Listener
            qrCodeLoginListener = activity as QrCodeLoginListener
        }
    }
}
