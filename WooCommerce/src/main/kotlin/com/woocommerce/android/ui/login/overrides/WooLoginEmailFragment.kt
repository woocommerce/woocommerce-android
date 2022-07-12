package com.woocommerce.android.ui.login.overrides

import android.content.Context
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.util.WooPermissionUtils.hasCameraPermission
import com.woocommerce.android.util.WooPermissionUtils.requestCameraPermission
import org.wordpress.android.login.LoginEmailFragment

class WooLoginEmailFragment : LoginEmailFragment() {
    interface Listener {
        fun onWhatIsWordPressLinkClicked()
        fun onQrCodeLoginClicked()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                whatIsWordPressLinkClickListener.onQrCodeLoginClicked()
            } else showCameraPermissionDeniedDialog()
        }


    private lateinit var whatIsWordPressLinkClickListener: Listener

    @LayoutRes
    override fun getContentLayout(): Int = R.layout.fragment_login_email_screen

    override fun setupContent(rootView: ViewGroup) {
        super.setupContent(rootView)
        rootView.findViewById<Button>(R.id.login_what_is_wordpress).setOnClickListener {
            whatIsWordPressLinkClickListener.onWhatIsWordPressLinkClicked()
        }

        rootView.findViewById<Button>(R.id.button_login_qr_code).setOnClickListener {
            if (hasCameraPermission(requireContext())) {
                whatIsWordPressLinkClickListener.onQrCodeLoginClicked()
            } else requestCameraPermission(requestPermissionLauncher)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            whatIsWordPressLinkClickListener = activity as Listener
        }
    }

    private fun showCameraPermissionDeniedDialog() {
        WooDialog.showDialog(
            requireActivity(),
            titleId = R.string.qr_code_login_camera_permission_denied_title,
            messageId = R.string.qr_code_login_camera_permission_denied_message,
            positiveButtonId = R.string.qr_code_login_edit_camera_permission,
            negativeButtonId = R.string.cancel,
            posBtnAction = { _, _ -> WooPermissionUtils.showAppSettings(requireContext()) },
            negBtnAction = { dialog, _ -> dialog.dismiss() },
        )
    }
}
