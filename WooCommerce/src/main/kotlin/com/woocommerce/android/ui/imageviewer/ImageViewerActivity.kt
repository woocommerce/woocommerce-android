package com.woocommerce.android.ui.imageviewer

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.activity_image_viewer.*
import org.wordpress.android.fluxc.model.WCProductImageModel

/**
 * Full-screen image view with pinch-and-zoom
 */
class ImageViewerActivity : AppCompatActivity(), RequestListener<Drawable> {
    companion object {
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_IMAGE_TITLE = "image_title"
        private const val KEY_IMAGE_REMOTE_PRODUCT_ID = "remote_product_id"
        private const val KEY_IMAGE_REMOTE_MEDIA_ID = "remote_media_id"
        private const val KEY_TRANSITION_NAME = "transition_name"
        private const val TOOLBAR_FADE_DELAY_MS = 2500L

        fun showProductImage(
            activity: Activity,
            productModel: Product,
            imageModel: WCProductImageModel,
            sharedElement: View? = null
        ) {
            val intent = Intent(activity, ImageViewerActivity::class.java).also {
                it.putExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, productModel.remoteId)
                it.putExtra(KEY_IMAGE_REMOTE_MEDIA_ID, imageModel.id)
                it.putExtra(KEY_IMAGE_URL, imageModel.src)

                if (imageModel.name.isNotEmpty()) {
                    it.putExtra(KEY_IMAGE_TITLE, imageModel.name)
                } else {
                    it.putExtra(KEY_IMAGE_TITLE, productModel.name)
                }
            }

            // use a shared element transition if a shared element view was passed, otherwise default to fade-in
            val options = if (sharedElement != null && sharedElement.transitionName.isNotEmpty()) {
                val transitionName = sharedElement.transitionName
                intent.putExtra(KEY_TRANSITION_NAME, transitionName)
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

    private var remoteProductId = 0L
    private var remoteMediaId = 0L
    private lateinit var imageUrl: String
    private lateinit var imageTitle: String
    private lateinit var transitionName: String

    private val fadeOutToolbarHandler = Handler()
    private var canTransitionOnFinish = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        remoteProductId = if (savedInstanceState == null) {
            intent.getLongExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, 0L)
        } else {
            savedInstanceState.getLong(KEY_IMAGE_REMOTE_PRODUCT_ID)
        }

        remoteMediaId = if (savedInstanceState == null) {
            intent.getLongExtra(KEY_IMAGE_REMOTE_MEDIA_ID, 0L)
        } else {
            savedInstanceState.getLong(KEY_IMAGE_REMOTE_MEDIA_ID)
        }

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

        transitionName = if (savedInstanceState == null) {
            intent.getStringExtra(KEY_TRANSITION_NAME) ?: ""
        } else {
            savedInstanceState.getString(KEY_TRANSITION_NAME) ?: ""
        }
        photoView.transitionName = transitionName

        val toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40)
        toolbar.background = ColorDrawable(toolbarColor)
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.title = imageTitle
            it.setDisplayShowTitleEnabled(imageTitle.isNotEmpty())
            it.setDisplayHomeAsUpEnabled(true)
        }

        // PhotoView doesn't play nice with shared element transitions if we rotate before exiting, so we
        // use this variable to skip the transition if the activity is re-created
        canTransitionOnFinish = savedInstanceState == null

        loadImage()
        showToolbar(true)

        photoView.setOnPhotoTapListener { view, x, y ->
            showToolbar(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let { bundle ->
            bundle.putLong(KEY_IMAGE_REMOTE_PRODUCT_ID, remoteProductId)
            bundle.putLong(KEY_IMAGE_REMOTE_MEDIA_ID, remoteMediaId)
            bundle.putString(KEY_IMAGE_URL, imageUrl)
            bundle.putString(KEY_IMAGE_TITLE, imageTitle)
            bundle.putString(KEY_TRANSITION_NAME, transitionName)
            super.onSaveInstanceState(outState)
        }
    }

    override fun finishAfterTransition() {
        if (canTransitionOnFinish) {
            super.finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (remoteProductId != 0L && remoteMediaId != 0L) {
            menuInflater.inflate(R.menu.menu_trash, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_trash -> {
                removeProductImage()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            } else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadImage() {
        showProgress(true)

        GlideApp.with(this)
                .load(imageUrl)
                .listener(this)
                .into(photoView)
    }

    private fun removeProductImage() {
        // TODO
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

            @AnimRes val animRes = if (show) {
                R.anim.toolbar_fade_in_and_down
            } else {
                R.anim.toolbar_fade_out_and_up
            }

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
                finish()
            }
        }
        Snackbar.make(snack_root, R.string.error_loading_image, Snackbar.LENGTH_SHORT)
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
