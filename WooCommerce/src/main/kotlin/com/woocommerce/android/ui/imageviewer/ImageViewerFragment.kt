package com.woocommerce.android.ui.imageviewer

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.fragment_image_viewer.*
import org.wordpress.android.fluxc.model.WCProductImageModel

class ImageViewerFragment : androidx.fragment.app.Fragment(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_IMAGE_TITLE = "image_title"
        private const val KEY_IMAGE_REMOTE_MEDIA_ID = "remote_media_id"

        interface ImageViewerListener {
            fun onImageTapped()
        }

        fun newInstance(imageModel: WCProductImageModel): ImageViewerFragment {
            val args = Bundle().also {
                it.putString(KEY_IMAGE_URL, imageModel.src)
                it.putString(KEY_IMAGE_TITLE, imageModel.name)
                it.putLong(KEY_IMAGE_REMOTE_MEDIA_ID, imageModel.id)
            }
            ImageViewerFragment().also {
                it.arguments = args
                return it
            }
        }
    }

    private var remoteMediaId = 0L
    private lateinit var imageUrl: String
    private lateinit var imageTitle: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteMediaId = arguments?.getLong(KEY_IMAGE_REMOTE_MEDIA_ID, 0L) ?: 0L
        imageUrl = arguments?.getString(KEY_IMAGE_URL)?: ""
        imageTitle = arguments?.getString(KEY_IMAGE_TITLE) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadImage()
        photoView.setOnPhotoTapListener { view, x, y ->
            (activity as? ImageViewerListener)?.onImageTapped()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(KEY_IMAGE_REMOTE_MEDIA_ID, remoteMediaId)
        outState.putString(KEY_IMAGE_URL, imageUrl)
        outState.putString(KEY_IMAGE_TITLE, imageTitle)
        super.onSaveInstanceState(outState)
    }

    private fun loadImage() {
        showProgress(true)

        GlideApp.with(this)
                .load(imageUrl)
                .listener(this)
                .into(photoView)
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Glide failed to load the image, show a snackbar alerting user to the error and finish after it's dismissed
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        val callback = object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                super.onDismissed(transientBottomBar, event)
                // TODO: finish()
            }
        }
        Snackbar.make(photoViewContainer, R.string.error_loading_image, Snackbar.LENGTH_SHORT)
                .addCallback(callback)
                .show()
        return false
    }

    /**
     * Glide has loaded the image, hide the progress bar and add our photo attacher which enables pinch/zoom
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
