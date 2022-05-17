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

        private const val RATIO_PORTRAIT = 0.6f
        private const val RATIO_LANDSCAPE = 0.2f

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
        arguments?.let { args ->
            val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
            binding.textView.setText(args.getInt(ARG_STRING_ID))

            val isLandscape = DisplayUtils.isLandscape(context)
            val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)

            // hide images in landscape unless this device is a tablet
            if (isLandscape && !isTablet) {
                binding.imageView.hide()
            } else {
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            }

            // adjust the view sizes based on orientation
            val ratio = if (isLandscape) {
                (DisplayUtils.getWindowPixelWidth(requireContext()) * RATIO_LANDSCAPE).toInt()
            } else {
                (DisplayUtils.getWindowPixelWidth(requireContext()) * RATIO_PORTRAIT).toInt()
            }
            binding.textView.layoutParams.width = ratio
            binding.imageView.layoutParams.width = ratio
        }
    }
}
