package com.woocommerce.android.ui.login

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
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
        private const val ARG_TITLE_ID = "title_id"
        private const val ARG_SUBTITLE_ID = "subtitle_id"

        fun newInstance(
            @DrawableRes drawableId: Int,
            @StringRes titleId: Int,
            @StringRes subtitleId: Int
        ): LoginPrologueViewPagerItemFragment {
            LoginPrologueViewPagerItemFragment().also { fragment ->
                fragment.arguments = Bundle().also { bundle ->
                    bundle.putInt(ARG_DRAWABLE_ID, drawableId)
                    bundle.putInt(ARG_TITLE_ID, titleId)
                    bundle.putInt(ARG_SUBTITLE_ID, subtitleId)
                }
                return fragment
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let { args ->
            val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
            binding.prologueTitle.setText(args.getInt(ARG_TITLE_ID))
            binding.prologueSubtitle.setText(args.getInt(ARG_SUBTITLE_ID))

            val isLandscape = DisplayUtils.isLandscape(context)
            val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)

            // hide images in landscape unless this device is a tablet
            if (isLandscape && !isTablet) {
                binding.imageView.hide()
            } else {
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            }

            // Adjust the view sizes based on orientation
            if (isLandscape || isTablet) {
                val ratio = ResourcesCompat.getFloat(resources, R.dimen.prologue_width_percent)
                val width = (DisplayUtils.getWindowPixelWidth(requireContext()) * ratio).toInt()
                binding.prologueTitle.maxWidth = width
                binding.prologueSubtitle.maxWidth = width
            }
        }
    }
}
