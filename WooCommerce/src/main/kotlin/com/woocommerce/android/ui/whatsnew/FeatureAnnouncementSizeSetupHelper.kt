package com.woocommerce.android.ui.whatsnew

import android.util.TypedValue
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment.STYLE_NO_TITLE
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woocommerce.android.R
import javax.inject.Inject

class FeatureAnnouncementSizeSetupHelper @Inject constructor(
    private val displayAsDialog: FeatureAnnouncementDisplayAsDialog
) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        val fragment = owner as FeatureAnnouncementDialogFragment
        if (displayAsDialog()) {
            fragment.setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
        } else {
            fragment.setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val fragment = owner as FeatureAnnouncementDialogFragment
        if (displayAsDialog()) {
            val dialogHeightPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                DIALOG_HEIGHT_DP.toFloat(),
                fragment.resources.displayMetrics
            )
            fragment.dialog?.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dialogHeightPx.toInt()
            )
        } else {
            fragment.dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private companion object {
        const val DIALOG_HEIGHT_DP = 450
    }
}
