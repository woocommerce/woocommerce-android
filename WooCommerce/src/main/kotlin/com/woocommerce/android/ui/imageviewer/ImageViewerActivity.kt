package com.woocommerce.android.ui.imageviewer

import android.app.Activity
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
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.activity_image_viewer.*
import org.wordpress.android.util.ToastUtils

/**
 * Full-screen image view with pinch-and-zoom
 */
class ImageViewerActivity : AppCompatActivity(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_IMAGE_TITLE = "image_title"
        private const val TOOLBAR_FADE_DELAY_MS = 2500L

        fun show(activity: Activity, imageUrl: String, title: String = "", sharedElement: View? = null) {
            val intent = Intent(activity, ImageViewerActivity::class.java)
            intent.putExtra(KEY_IMAGE_URL, imageUrl)
            intent.putExtra(KEY_IMAGE_TITLE, title)

            val transitionName = activity.getString(R.string.shared_element_transition)

            // use a shared element transition if a shared element view was passed, otherwise default to fade-in
            val options = if (sharedElement != null) {
                ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElement, transitionName)
            } else {
                ActivityOptionsCompat.makeCustomAnimation(
                        activity,
                        R.anim.activity_fade_in,
                        R.anim.activity_fade_out
                )
            }
            ActivityCompat.startActivity(activity, intent, options.toBundle())
        }
    }

    private lateinit var imageUrl: String
    private lateinit var imageTitle: String

    private val fadeOutToolbarHandler = Handler()
    private var canTransitionOnFinish = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        imageUrl = if (savedInstanceState == null) {
            intent.getStringExtra(KEY_IMAGE_URL) ?: ""
        } else {
            savedInstanceState.getString(KEY_IMAGE_URL) ?: ""
        }

        imageTitle = if (savedInstanceState == null) {
            intent.getStringExtra(KEY_IMAGE_TITLE) ?: ""
        } else {
            savedInstanceState.getString(KEY_IMAGE_TITLE) ?: ""
        }

        val toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40)
        toolbar.background = ColorDrawable(toolbarColor)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.title = imageTitle
            it.setDisplayShowTitleEnabled(imageTitle.isNotEmpty())
            it.setDisplayHomeAsUpEnabled(true)
        }

        // PhotoViewAttacher doesn't play nice with shared element transitions if we rotate before exiting, so
        // we use this variable to skip the transition if the activity is re-created
        canTransitionOnFinish = savedInstanceState == null

        loadImage()
        showToolbar(true)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(KEY_IMAGE_URL, imageUrl)
        outState?.putString(KEY_IMAGE_TITLE, imageTitle)
        super.onSaveInstanceState(outState)
    }

    override fun finishAfterTransition() {
        if (canTransitionOnFinish) {
            super.finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadImage() {
        showProgress(true)

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
            // remove the current fade-out runnable and start a new one to hide the toolbar shortly after we show it
            fadeOutToolbarHandler.removeCallbacks(fadeOutToolbarRunnable)
            if (show) {
                fadeOutToolbarHandler.postDelayed(fadeOutToolbarRunnable, TOOLBAR_FADE_DELAY_MS)
            }

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
        ToastUtils.showToast(this, R.string.error_loading_image)
        finish()
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

        val attacher = PhotoViewAttacher(image)
        attacher.setOnPhotoTapListener { view, x, y ->
            showToolbar(true)
        }

        return false
    }
}
