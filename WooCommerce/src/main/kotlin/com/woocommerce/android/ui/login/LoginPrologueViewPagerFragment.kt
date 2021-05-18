package com.woocommerce.android.ui.login

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentImageViewerBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product

/**
 * Displays a single image in the login prologue view pager
 */
class LoginPrologueViewPagerFragment : Fragment(R.layout.fragment_login_prologue_viewpager_item) {
    companion object {
        private const val ARG_DRAWABLE_ID = "drawable_id"

        fun newInstance(@DrawableRes drawableId: Int): LoginPrologueViewPagerFragment {
            val args = Bundle().also {
                it.putInt(ARG_DRAWABLE_ID, drawableId)
            }
            LoginPrologueViewPagerFragment().also {
                it.arguments = args
                return it
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val drawableId = arguments?.getInt(ARG_DRAWABLE_ID)
        // val binding = FragmentLoginPrologueViewPagerItem.bind(view)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
