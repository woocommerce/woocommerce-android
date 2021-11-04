package com.woocommerce.android.ui.orders.quickorder

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogQuickOrderBinding
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils

@AndroidEntryPoint
class QuickOrderDialog : DialogFragment(R.layout.dialog_quick_order) {
    companion object {
        private const val WIDTH_RATIO = 0.35f
        private const val HEIGHT_RATIO = 0.8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogQuickOrderBinding.bind(view)
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window!!.setLayout(
            (DisplayUtils.getDisplayPixelWidth() * WIDTH_RATIO).toInt(),
            (DisplayUtils.getDisplayPixelHeight(context) * HEIGHT_RATIO).toInt()
        )
    }
}
