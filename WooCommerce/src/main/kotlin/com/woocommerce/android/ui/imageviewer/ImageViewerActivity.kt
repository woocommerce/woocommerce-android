package com.woocommerce.android.ui.imageviewer

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.activity_image_viewer.*
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import uk.co.senab.photoview.PhotoViewAttacher

class ImageViewerActivity : AppCompatActivity(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IMAGE_URL = "image_url"

        fun show(context: Context, imageUrl: String) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            intent.putExtra(KEY_IMAGE_URL, imageUrl)
            context.startActivity(intent)
        }
    }

    private lateinit var imageUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        imageUrl = if (savedInstanceState == null) {
            intent.getStringExtra(KEY_IMAGE_URL) ?: ""
        } else {
            savedInstanceState.getString(KEY_IMAGE_URL) ?: ""
        }

        loadImage()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(KEY_IMAGE_URL, imageUrl)
        super.onSaveInstanceState(outState)
    }

    private fun loadImage() {
        showProgress(true)

        val imageWidth = DisplayUtils.getDisplayPixelWidth(this)
        val imageHeight = DisplayUtils.getDisplayPixelWidth(this)
        val imageSize = Math.max(imageWidth, imageHeight)
        val imageUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, 0)

        GlideApp.with(this)
                .load(imageUrl)
                .listener(this)
                .into(image)
    }

    private fun showProgress(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        return false
    }

    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        val attacher = PhotoViewAttacher(image)
        return false
    }
}
