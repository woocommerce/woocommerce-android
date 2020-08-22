package com.woocommerce.android.ui.wpmediapicker

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.fragment_wpmedia_viewer.*

/**
 * Fullscreen single image viewer
 */
class WPMediaViewerFragment : androidx.fragment.app.Fragment(), RequestListener<Drawable> {
    private val navArgs: WPMediaViewerFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wpmedia_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        iconBack.setOnClickListener {
            findNavController().navigateUp()
        }
        loadImage()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun loadImage() {
        progressBar.isVisible = true
        GlideApp.with(this)
                .load(navArgs.imageUrl)
                .listener(this)
                .into(photoView)
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
        progressBar.isVisible = false
        return false
    }

    /**
     * Glide has loaded the image, hide the progress bar
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        progressBar.isVisible = false
        return false
    }
}
