package com.woocommerce.android.ui.products

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.fragment_image_viewer.*

/**
 * Single image viewer used by the ViewPager in [ProductImagesFragment]
 */
class ImageViewerFragment : androidx.fragment.app.Fragment(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IMAGE_URL = "image_url"

        interface ImageViewerListener {
            fun onImageTapped()
            fun onImageLoadError()
        }

        fun newInstance(imageModel: Product.Image): ImageViewerFragment {
            val args = Bundle().also {
                it.putString(KEY_IMAGE_URL, imageModel.source)
            }
            ImageViewerFragment().also {
                it.arguments = args
                return it
            }
        }
    }

    private lateinit var imageUrl: String
    private var imageListener: ImageViewerListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageUrl = arguments?.getString(KEY_IMAGE_URL) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadImage()
        photoView.setOnPhotoTapListener { _, _, _ ->
            imageListener?.onImageTapped()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(KEY_IMAGE_URL, imageUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    fun setImageListener(listener: ImageViewerListener) {
        imageListener = listener
    }

    private fun loadImage() {
        showProgress(true)

        GlideApp.with(this)
                .load(imageUrl)
                .listener(this)
                .into(photoView)
    }

    private fun showProgress(show: Boolean) {
        progressBar.isVisible = show
    }

    /**
     * Glide failed to load the image, alert the host activity
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        imageListener?.onImageLoadError()
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
        showProgress(false)
        return false
    }
}
