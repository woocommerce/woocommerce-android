package com.woocommerce.android.ui.login

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentLoginPrologueViewpagerItemBinding
import org.wordpress.android.util.DisplayUtils

/**
 * Displays a single image and text label in the login prologue view pager
 */
class LoginPrologueViewPagerItemFragment : Fragment(R.layout.fragment_login_prologue_viewpager_item) {
    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"
        private const val ARG_STRING_ID = "string_id"
        private const val ARG_SHOW_IMAGES = "show_images"

        fun newInstance(
            @DrawableRes drawableId: Int,
            @StringRes stringId: Int,
            showImages: Boolean
        ): LoginPrologueViewPagerItemFragment {
            LoginPrologueViewPagerItemFragment().also {
                it.arguments = Bundle().also {
                    it.putInt(ARG_DRAWABLE_ID, drawableId)
                    it.putInt(ARG_STRING_ID, stringId)
                    it.putBoolean(ARG_SHOW_IMAGES, showImages)
                }
                return it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
        arguments?.let { args ->
            if (args.getBoolean(ARG_SHOW_IMAGES)) {
                binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            } else {
                binding.imageView.visibility = View.GONE
            }
            binding.textView.setText(args.getInt(ARG_STRING_ID))
            binding.textView.layoutParams.width = if (DisplayUtils.isLandscape(context)) {
                (DisplayUtils.getDisplayPixelWidth() * 0.45).toInt()
            } else {
                (DisplayUtils.getDisplayPixelWidth() * 0.65).toInt()
            }
        }
    }
}
