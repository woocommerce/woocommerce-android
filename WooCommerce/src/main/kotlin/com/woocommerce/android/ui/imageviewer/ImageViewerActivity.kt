package com.woocommerce.android.ui.imageviewer

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.R.style
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.imageviewer.ImageViewerFragment.Companion.ImageViewerListener
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_image_viewer.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

/**
 * Full-screen product image viewer with pinch-and-zoom
 */
class ImageViewerActivity : AppCompatActivity(), ImageViewerListener {
    companion object {
        const val EXTRA_REMOVE_REMOTE_IMAGE_ID = "remove_remote_media_id"

        private const val KEY_IMAGE_REMOTE_MEDIA_ID = "remote_media_id"
        private const val KEY_IMAGE_REMOTE_PRODUCT_ID = "remote_product_id"
        private const val KEY_TRANSITION_NAME = "transition_name"
        private const val KEY_ENABLE_REMOVE_IMAGE = "enable_remove_image"
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"

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
                it.putExtra(KEY_ENABLE_REMOVE_IMAGE, enableRemoveImage)
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

    @Inject lateinit var productStore: WCProductStore
    @Inject lateinit var selectedSite: SelectedSite

    private var remoteProductId = 0L
    private var remoteMediaId = 0L
    private var enableRemoveImage = false

    private lateinit var transitionName: String
    private lateinit var images: List<WCProductImageModel>

    private val fadeOutToolbarHandler = Handler()
    private var canTransitionOnFinish = true

    private var confirmationDialog: AlertDialog? = null
    private var isConfirmationShowing = false

    private var adapter: ImageViewerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        remoteProductId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_PRODUCT_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, 0L)

        remoteMediaId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_MEDIA_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_MEDIA_ID, 0L)

        enableRemoveImage = savedInstanceState?.getBoolean(KEY_ENABLE_REMOVE_IMAGE)
                ?: intent.getBooleanExtra(KEY_ENABLE_REMOVE_IMAGE, false)

        transitionName = savedInstanceState?.getString(KEY_TRANSITION_NAME)
                ?: intent.getStringExtra(KEY_TRANSITION_NAME) ?: ""
        container.transitionName = transitionName

        productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)?.let {
            images = it.getImages()
        }
        // TODO: handle null product

        val toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40)
        toolbar.background = ColorDrawable(toolbarColor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // PhotoView doesn't play nice with shared element transitions if we rotate before exiting, so we
        // use this variable to skip the transition if the activity is re-created
        canTransitionOnFinish = savedInstanceState == null

        showToolbar(true)

        if (savedInstanceState?.getBoolean(KEY_IS_CONFIRMATION_SHOWING) == true) {
            confirmRemoveProductImage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.let { bundle ->
            bundle.putLong(KEY_IMAGE_REMOTE_PRODUCT_ID, remoteProductId)
            bundle.putLong(KEY_IMAGE_REMOTE_MEDIA_ID, remoteMediaId)
            bundle.putString(KEY_TRANSITION_NAME, transitionName)
            bundle.putBoolean(KEY_ENABLE_REMOVE_IMAGE, enableRemoveImage)
            bundle.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
            super.onSaveInstanceState(outState)
        }
    }

    override fun onPause() {
        confirmationDialog?.dismiss()
        super.onPause()
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

    override fun onImageTapped() {
        showToolbar(true)
    }

    /**
     * Confirms that the user meant to remove this image from the product - the actual removal must be
     * done in the calling activity
     */
    private fun confirmRemoveProductImage() {
        isConfirmationShowing = true
        confirmationDialog = AlertDialog.Builder(ContextThemeWrapper(this, style.AppTheme))
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
                .setNegativeButton(R.string.dont_remove, { _, _ ->
                    isConfirmationShowing = false
                })
                .show()
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

    private fun setupViewPager() {
        adapter = ImageViewerAdapter(supportFragmentManager)
        viewPager.setAdapter(adapter)
        // TODO viewPager.setPageTransformer(false, WPViewPagerTransformer(TransformType.SLIDE_OVER))

        // determine the position of the original media item so we can page to it immediately
        for (index in images.indices) {
            if (remoteMediaId == images[index].id) {
                viewPager.setCurrentItem(index)
                break
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                showToolbar(true)
                remoteMediaId = images[position].id
            }
        })
    }

    internal inner class ImageViewerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            val fragment = ImageViewerFragment.newInstance(images[position])
            return fragment
        }

        override fun getCount(): Int {
            return images.size
        }
    }
}
