package com.woocommerce.android.ui.login.qrcode

import android.app.Activity
import com.woocommerce.android.R
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.util.WooPermissionUtils

fun showCameraPermissionDeniedDialog(activity: Activity) {
    WooDialog.showDialog(
        activity,
        titleId = R.string.qr_code_login_camera_permission_denied_title,
        messageId = R.string.qr_code_login_camera_permission_denied_message,
        positiveButtonId = R.string.qr_code_login_edit_camera_permission,
        negativeButtonId = R.string.cancel,
        posBtnAction = { dialog, _ ->
            WooPermissionUtils.showAppSettings(activity)
            dialog.dismiss()
        },
        negBtnAction = { dialog, _ -> dialog.dismiss() },
    )
}
