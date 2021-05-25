package com.woocommerce.android.ui.login

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentLoginPrologueViewpagerItemBinding
import com.woocommerce.android.extensions.hide
import org.wordpress.android.util.DisplayUtils

/**
 * Displays a single image and text label in the login prologue view pager
 */
class LoginPrologueViewPagerItemFragment : Fragment(R.layout.fragment_login_prologue_viewpager_item) {
    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"
        private const val ARG_STRING_ID = "string_id"

        private const val LABEL_RATIO_PORTRAIT = 0.65f
        private const val LABEL_RATIO_LANDSCAPE = 0.45f
        private const val IMAGE_RATIO_PORTRAIT = 0.65f
        private const val IMAGE_RATIO_LANDSCAOE = 0.35f

        fun newInstance(
            @DrawableRes drawableId: Int,
            @StringRes stringId: Int
        ): LoginPrologueViewPagerItemFragment {
            LoginPrologueViewPagerItemFragment().also { fragment ->
                fragment.arguments = Bundle().also { bundle ->
                    bundle.putInt(ARG_DRAWABLE_ID, drawableId)
                    bundle.putInt(ARG_STRING_ID, stringId)
                }
                return fragment
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // hide images in landscape unless this device is a tablet
        val isLandscape = DisplayUtils.isLandscape(context)
        val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
        val hideImages = isLandscape && !isTablet

        arguments?.let { args ->
            val screenWidth = DisplayUtils.getDisplayPixelWidth()
            val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)

            if (hideImages) {
                binding.imageView.hide()
            } else {
                binding.imageView.layoutParams.width = if (isLandscape) {
                    (screenWidth * IMAGE_RATIO_LANDSCAOE).toInt()
                } else {
                    (screenWidth * IMAGE_RATIO_PORTRAIT).toInt()
                }
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            }
            binding.textView.layoutParams.width = if (isLandscape) {
                (screenWidth * LABEL_RATIO_LANDSCAPE).toInt()
            } else {
                (screenWidth * LABEL_RATIO_PORTRAIT).toInt()
            }
            binding.textView.setText(args.getInt(ARG_STRING_ID))
        }
    }
}
