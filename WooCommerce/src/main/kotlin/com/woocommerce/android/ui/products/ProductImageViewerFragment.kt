package com.woocommerce.android.ui.products

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.fragment_image_viewer.*

class ProductImageViewerFragment : BaseProductFragment(), RequestListener<Drawable> {
    private val navArgs: ProductImageViewerFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onRequestAllowBackPress(): Boolean {
        return true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadImage()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun loadImage() {
        showProgress(true)

        GlideApp.with(this)
                .load(navArgs.imageUrl)
                .listener(this)
                .into(photoView)
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Glide failed to load the image
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        uiMessageResolver.showSnack(R.string.error_loading_image)
        findNavController().navigateUp()
        return false
    }

    /**
     * Glide has loaded the image
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        return false
    }
}
