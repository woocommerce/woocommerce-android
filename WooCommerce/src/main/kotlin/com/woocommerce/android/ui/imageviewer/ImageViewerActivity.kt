package com.woocommerce.android.ui.imageviewer

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.support.annotation.AnimRes
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
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
        private const val FADE_DELAY_MS = 3000L

        fun show(context: Context, imageUrl: String) {
            val intent = Intent(context, ImageViewerActivity::class.java)
            intent.putExtra(KEY_IMAGE_URL, imageUrl)
            val options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.activity_fade_in,
                    R.anim.activity_fade_out
            )
            ActivityCompat.startActivity(context, intent, options.toBundle())
        }
    }

    private lateinit var imageUrl: String

    private val fadeOutToolbarHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        val toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40)
        toolbar.background = ColorDrawable(toolbarColor)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        imageUrl = if (savedInstanceState == null) {
            intent.getStringExtra(KEY_IMAGE_URL) ?: ""
        } else {
            savedInstanceState.getString(KEY_IMAGE_URL) ?: ""
        }

        loadImage()
        showToolbar(true)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(KEY_IMAGE_URL, imageUrl)
        super.onSaveInstanceState(outState)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.activity_fade_in, R.anim.activity_fade_out)
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

    private fun showToolbar(show: Boolean) {
        if (!isFinishing) {
            fadeOutToolbarHandler.removeCallbacks(fadeOutToolbarRunnable)
            fadeOutToolbarHandler.postDelayed(fadeOutToolbarRunnable, FADE_DELAY_MS)

            if ((show && toolbar.visibility == View.VISIBLE) || (!show && toolbar.visibility != View.VISIBLE)) {
                return
            }

            @AnimRes val animRes = if (show)
                R.anim.toolbar_fade_in_and_down
            else
                R.anim.toolbar_fade_out_and_up

            val listener = object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    if (show) toolbar.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animation) {
                    if (!show) toolbar.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation) {
                    // noop
                }
            }

            AnimationUtils.loadAnimation(this, animRes)?.let { anim ->
                anim.setAnimationListener(listener)
                toolbar.startAnimation(anim)
            }
        }
    }

    private val fadeOutToolbarRunnable = Runnable {
        showToolbar(false)
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

    /**
     * Image has been loaded, hide the progress bar and add our photo attacher which enables pinch/zoom
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        showProgress(false)
        val attacher = PhotoViewAttacher(image)
        attacher.setOnPhotoTapListener { view, x, y ->
            showToolbar(true)
        }
        return false
    }
}
