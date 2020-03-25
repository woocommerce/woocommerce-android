package com.woocommerce.android.ui.imageviewer

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.activity.viewModels
import androidx.annotation.AnimRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.imageviewer.ImageViewerFragment.Companion.ImageViewerListener
import com.woocommerce.android.viewmodel.ViewModelFactory
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_image_viewer.*
import javax.inject.Inject

/**
 * Full-screen product image viewer with pinch-and-zoom
 */
class ImageViewerActivity : AppCompatActivity(), ImageViewerListener {
    companion object {
        private const val KEY_IMAGE_REMOTE_MEDIA_ID = "remote_media_id"
        private const val KEY_IMAGE_REMOTE_PRODUCT_ID = "remote_product_id"
        private const val KEY_TRANSITION_NAME = "transition_name"

        private const val TOOLBAR_FADE_DELAY_MS = 2500L

        /**
         * Shows all images for the passed product in a swipeable viewPager - the viewPager
         * will automatically position itself to the passed image
         */
        fun showProductImages(
            fragment: Fragment,
            productModel: Product,
            imageModel: Product.Image,
            sharedElement: View? = null
        ) {
            val context = fragment.requireActivity()

            val intent = Intent(context, ImageViewerActivity::class.java).also {
                it.putExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, productModel.remoteId)
                it.putExtra(KEY_IMAGE_REMOTE_MEDIA_ID, imageModel.id)
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

            fragment.startActivityForResult(intent, RequestCodes.PRODUCT_IMAGE_VIEWER, options.toBundle())
        }
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var remoteProductId = 0L
    private var remoteMediaId = 0L

    private lateinit var transitionName: String
    private val viewModel: ImageViewerViewModel by viewModels { viewModelFactory }
    private lateinit var pagerAdapter: ImageViewerAdapter

    private val fadeOutToolbarHandler = Handler()
    private var canTransitionOnFinish = true

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        remoteProductId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_PRODUCT_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_PRODUCT_ID, 0L)

        remoteMediaId = savedInstanceState?.getLong(KEY_IMAGE_REMOTE_MEDIA_ID)
                ?: intent.getLongExtra(KEY_IMAGE_REMOTE_MEDIA_ID, 0L)

        transitionName = savedInstanceState?.getString(KEY_TRANSITION_NAME)
                ?: intent.getStringExtra(KEY_TRANSITION_NAME) ?: ""
        container.transitionName = transitionName

        val toolbarColor = ContextCompat.getColor(this, R.color.black_translucent_40)
        toolbar.background = ColorDrawable(toolbarColor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = null

        // PhotoView doesn't play nice with shared element transitions if we rotate before exiting, so we
        // use this variable to skip the transition if the activity is re-created
        canTransitionOnFinish = savedInstanceState == null

        showToolbar(true)

        initializeViewModel()
    }

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(remoteProductId)
    }

    private fun setupObservers(viewModel: ImageViewerViewModel) {
        viewModel.product.observe(this, Observer { product ->
            if (product.images.isEmpty()) {
                finishAfterTransition()
            } else {
                setupViewPager(product.images)
            }
        })

        viewModel.showSnackbarMessage.observe(this, Observer { msgId ->
            uiMessageResolver.showSnack(msgId)
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.let { bundle ->
            bundle.putLong(KEY_IMAGE_REMOTE_PRODUCT_ID, remoteProductId)
            bundle.putLong(KEY_IMAGE_REMOTE_MEDIA_ID, remoteMediaId)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    override fun onImageLoadError() {
        uiMessageResolver.showSnack(R.string.error_loading_image)
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

    private fun setupViewPager(images: List<Product.Image>) {
        pagerAdapter = ImageViewerAdapter(supportFragmentManager, images)
        viewPager.adapter = pagerAdapter
        viewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_large)

        // determine the position of the original media item so we can page to it immediately
        for (index in images.indices) {
            if (remoteMediaId == images[index].id) {
                viewPager.currentItem = index
                break
            }
        }

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                showToolbar(true)
                // don't add an exit transition if the user swiped to another image
                canTransitionOnFinish = false
                // remember this image id so we can return to it upon rotation, and so
                // we use the right image if the user requests to remove it
                remoteMediaId = images[position].id
            }
        })
    }

    internal inner class ImageViewerAdapter(fm: FragmentManager, val images: List<Product.Image>) :
            FragmentStatePagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return ImageViewerFragment.newInstance(images[position])
        }

        override fun getCount(): Int {
            return images.size
        }
    }
}
