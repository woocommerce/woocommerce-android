package com.woocommerce.android.ui.imageviewer

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
 * Full-screen product image viewer with pinch-and-zoom
 */
class ImageViewerActivity : AppCompatActivity(), RequestListener<Drawable> {
    companion object {
        const val EXTRA_REMOVE_REMOTE_IMAGE_ID = "remove_remote_media_id"

        private const val KEY_IMAGE_REMOTE_MEDIA_ID = "remote_media_id"
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_IMAGE_TITLE = "image_title"
        private const val KEY_IMAGE_REMOTE_PRODUCT_ID = "remote_product_id"
        private const val KEY_TRANSITION_NAME = "transition_name"
        private const val KEY_ENABLE_REMOVE_IMAGE = "enable_remove_image"

        private const val TOOLBAR_FADE_DELAY_MS = 2500L

        fun showProductImage(
            fragment: Fragment,
            productModel: Product,
            imageModel: WCProductImageModel,
            sharedElement: View? = null,
            enableRemoveImage: Boolean = false,
            requestCode: Int = 0
        ) {
            val context = fragment.requireActivity()

            val intent = Intent(context, ImageViewerActivity::class.java).also {
                it.putExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, productModel.remoteId)
                it.putExtra(KEY_IMAGE_REMOTE_MEDIA_ID, imageModel.id)
                it.putExtra(KEY_IMAGE_URL, imageModel.src)
                it.putExtra(KEY_ENABLE_REMOVE_IMAGE, enableRemoveImage)

                if (imageModel.name.isNotEmpty()) {
                    it.putExtra(KEY_IMAGE_TITLE, imageModel.name)
                } else {
                    it.putExtra(KEY_IMAGE_TITLE, productModel.name)
                }
            }

            // use a shared element transition if a shared element view was passed, otherwise default to fade-in
            val options = sharedElement?.let {
                intent.putExtra(KEY_TRANSITION_NAME, it.transitionName)
                ActivityOptions.makeSceneTransitionAnimation(context, sharedElement, it.transitionName)
            } ?: ActivityOptions.makeCustomAnimation(
                    context,
                    R.anim.activity_fade_in,
                    R.anim.activity_fade_out
            )

            fragment.startActivityForResult(intent, requestCode, options.toBundle())
        }
    }

    private var remoteProductId = 0L
    private var remoteMediaId = 0L
    private var enableRemoveImage = false

    private lateinit var imageUrl: String
    private lateinit var imageTitle: String
    private lateinit var transitionName: String

    private val fadeOutToolbarHandler = Handler()
    private var canTransitionOnFinish = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image_viewer)

        remoteProductId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_PRODUCT_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, 0L)

        remoteMediaId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_MEDIA_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_MEDIA_ID, 0L)

        imageUrl = savedInstanceState?.getString(KEY_IMAGE_URL)
                ?: intent.getStringExtra(KEY_IMAGE_URL) ?: intent.getStringExtra(KEY_IMAGE_TITLE) ?: ""

        imageTitle = savedInstanceState?.getString(KEY_IMAGE_TITLE)
                ?: intent.getStringExtra(KEY_IMAGE_TITLE) ?: ""

        enableRemoveImage = savedInstanceState?.getBoolean(KEY_ENABLE_REMOVE_IMAGE)
                ?: intent.getBooleanExtra(KEY_ENABLE_REMOVE_IMAGE, false)

        transitionName = savedInstanceState?.getString(KEY_TRANSITION_NAME)
                ?: intent.getStringExtra(KEY_TRANSITION_NAME) ?: ""
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
            bundle.putBoolean(KEY_ENABLE_REMOVE_IMAGE, enableRemoveImage)
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
        if (enableRemoveImage) {
            menuInflater.inflate(R.menu.menu_trash, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_trash -> {
                confirmRemoveProductImage()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            } else -> {
                super.onOptionsItemSelected(item)
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

    /**
     * Confirms that the user meant to remove this image from the product - the actual removal must be
     * done in the calling activity
     */
    private fun confirmRemoveProductImage() {
        AlertDialog.Builder(this)
                .setMessage(R.string.product_image_remove_confirmation)
                .setCancelable(true)
                .setPositiveButton(R.string.remove) { _, _ ->
                    // let the calling fragment know that the user requested to remove this image
                    val data = Intent().also {
                        it.putExtra(EXTRA_REMOVE_REMOTE_IMAGE_ID, remoteMediaId)
                    }
                    setResult(Activity.RESULT_OK, data)
                    finishAfterTransition()
                }
                .setNegativeButton(R.string.dont_remove, null)
                .show()
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
