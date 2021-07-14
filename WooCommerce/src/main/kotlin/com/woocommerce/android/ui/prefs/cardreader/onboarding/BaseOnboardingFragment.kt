package com.woocommerce.android.ui.prefs.cardreader.onboarding

import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import org.wordpress.android.util.DisplayUtils

/**
 * Base fragment for all the onboarding screens
 */
abstract class BaseOnboardingFragment : Fragment {
    protected var illustration: ImageView? = null

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    override fun onStart() {
        super.onStart()

        // hide the illustration in landscape unless the device is a tablet
        val isLandscape = DisplayUtils.isLandscape(context)
        val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
        if (isLandscape && !isTablet) {
            illustration?.hide()
        } else {
            illustration?.show()
        }
    }

    fun onCancel() {
        findNavController().navigateUp()
    }
}
