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
 * Displays a single image and label in the login prologue view pager
 */
class LoginPrologueViewPagerItemFragment : Fragment(R.layout.fragment_login_prologue_viewpager_item) {
    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"
        private const val ARG_STRING_ID = "string_id"

        fun newInstance(@DrawableRes drawableId: Int, @StringRes stringId: Int): LoginPrologueViewPagerItemFragment {
            LoginPrologueViewPagerItemFragment().also {
                it.arguments = Bundle().also {
                    it.putInt(ARG_DRAWABLE_ID, drawableId)
                    it.putInt(ARG_STRING_ID, stringId)
                }
                return it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
        arguments?.let { args ->
            binding.imageView.setImageResource(args.getInt(ARG_DRAWABLE_ID))
            binding.textView.setText(args.getInt(ARG_STRING_ID))
            binding.textView.layoutParams.width = DisplayUtils.getDisplayPixelWidth() / 2
        }
    }
}
