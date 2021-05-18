package com.woocommerce.android.ui.login

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentLoginPrologueViewpagerItemBinding

/**
 * Displays a single image in the login prologue view pager
 */
class LoginPrologueViewPagerItemFragment : Fragment(R.layout.fragment_login_prologue_viewpager_item) {
    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"

        fun newInstance(@DrawableRes drawableId: Int): LoginPrologueViewPagerItemFragment {
            val args = Bundle().also {
                it.putInt(ARG_DRAWABLE_ID, drawableId)
            }
            LoginPrologueViewPagerItemFragment().also {
                it.arguments = args
                return it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentLoginPrologueViewpagerItemBinding.bind(view)
        arguments?.let { args ->
            val drawableId = args.getInt(ARG_DRAWABLE_ID)
            binding.imageView.setImageResource(drawableId)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
