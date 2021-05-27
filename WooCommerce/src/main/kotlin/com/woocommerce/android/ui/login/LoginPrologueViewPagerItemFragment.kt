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
        private const val PORTRAIT_RATIO = 0.65f
        private const val LANDSCAPE_RATIO = 0.45f

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
        val hideImages = isLandscape &&
            !DisplayUtils.isTablet(context) &&
            !DisplayUtils.isXLargeTablet(context)

        val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
        arguments?.let { args ->
            if (hideImages) {
                binding.imageView.hide()
            } else {
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            }
            binding.textView.layoutParams.width = if (isLandscape) {
                (DisplayUtils.getDisplayPixelWidth() * LANDSCAPE_RATIO).toInt()
            } else {
                (DisplayUtils.getDisplayPixelWidth() * PORTRAIT_RATIO).toInt()
            }
            binding.textView.setText(args.getInt(ARG_STRING_ID))
        }
    }
}
