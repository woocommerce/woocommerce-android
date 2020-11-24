package com.woocommerce.android.ui.products

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ImageViewerFragment.Companion.ImageViewerListener
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.fragment_product_image_viewer.*
import javax.inject.Inject
import dagger.Lazy

class ProductImageViewerFragment : BaseFragment(), ImageViewerListener, BackPressListener {
    @Inject lateinit var viewModelFactory: Lazy<ViewModelFactory>
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    companion object {
        private const val KEY_REMOTE_MEDIA_ID = "media_id"
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"
        private const val TOOLBAR_FADE_DELAY_MS = 2500L
    }

    private val navArgs: ProductImageViewerFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by navGraphViewModels(R.id.nav_graph_image_gallery) {
        viewModelFactory.get()
    }

    private var isConfirmationShowing = false
    private var confirmationDialog: AlertDialog? = null
    private val fadeOutToolbarHandler = Handler()

    private var remoteMediaId = 0L
    private lateinit var pagerAdapter: ImageViewerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteMediaId = savedInstanceState?.getLong(KEY_REMOTE_MEDIA_ID) ?: navArgs.mediaId

        setHasOptionsMenu(false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_image_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupViewPager()

        iconBack.setOnClickListener {
            findNavController().navigateUp()
        }

        if (navArgs.isDeletingAllowed) {
            iconTrash.setOnClickListener {
                AnalyticsTracker.track(Stat.PRODUCT_IMAGE_SETTINGS_DELETE_IMAGE_BUTTON_TAPPED)
                confirmRemoveProductImage()
            }
            iconTrash.isVisible = true
        } else {
            iconTrash.isVisible = false
        }

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRMATION_SHOWING)) {
                confirmRemoveProductImage()
            }
        }

        fadeOutToolbarHandler.postDelayed(fadeOutToolbarRunnable, TOOLBAR_FADE_DELAY_MS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
    }

    override fun onRequestAllowBackPress() = true

    private fun setupViewPager() {
        resetAdapter()

        viewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_large)

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                showToolbar(true)
                // remember this image id so we can return to it upon rotation, and so
                // we use the right image if the user requests to remove it
                remoteMediaId = pagerAdapter.images[position].id
            }
        })
    }

    private fun resetAdapter() {
        val images = ArrayList<Product.Image>()
        images.addAll(viewModel.images)

        pagerAdapter = ImageViewerAdapter(childFragmentManager, images)
        viewPager.adapter = pagerAdapter

        val position = pagerAdapter.indexOfImageId(remoteMediaId)
        if (position > -1) {
            viewPager.currentItem = position
        }
    }

    /**
     * Confirms that the user meant to remove this image from the product
     */
    private fun confirmRemoveProductImage() {
        isConfirmationShowing = true
        confirmationDialog = MaterialAlertDialogBuilder(ContextThemeWrapper(
                requireActivity(), R.style.Theme_Woo_Dialog))
                .setMessage(R.string.product_image_remove_confirmation)
                .setCancelable(true)
                .setPositiveButton(R.string.remove) { _, _ ->
                    removeCurrentImage()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmationShowing = false
                }
                .show()
    }

    private fun removeCurrentImage() {
        val newImageCount = pagerAdapter.count - 1
        val currentMediaId = remoteMediaId

        // determine the image to return to when the adapter is reloaded following the image removal
        val newMediaId = when {
            newImageCount == 0 -> {
                0
            }
            viewPager.currentItem > 0 -> {
                pagerAdapter.images[viewPager.currentItem - 1].id
            }
            else -> {
                pagerAdapter.images[viewPager.currentItem + 1].id
            }
        }

        // animate the viewPager out, then remove the image and animate it back in - this gives
        // the appearance of the removed image being tossed away
        with(WooAnimUtils.getScaleOutAnim(viewPager)) {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    remoteMediaId = newMediaId
                    viewModel.onImageRemoved(currentMediaId)
                    // animate it back in if there are any images left, others return to the previous fragment
                    if (newImageCount > 0) {
                        WooAnimUtils.scaleIn(viewPager)
                        resetAdapter()
                        showToolbar(true)
                    } else {
                        findNavController().navigateUp()
                    }
                }
            })
            start()
        }
    }

    private fun showToolbar(show: Boolean) {
        if (isAdded) {
            if ((show && fakeToolbar.visibility == View.VISIBLE) || (!show && fakeToolbar.visibility != View.VISIBLE)) {
                return
            }

            // remove the current fade-out runnable and start a new one to hide the toolbar shortly after we show it
            fadeOutToolbarHandler.removeCallbacks(fadeOutToolbarRunnable)
            if (show) {
                fadeOutToolbarHandler.postDelayed(fadeOutToolbarRunnable, TOOLBAR_FADE_DELAY_MS)
            }

            @AnimRes val animRes = if (show) {
                R.anim.toolbar_fade_in_and_down
            } else {
                R.anim.toolbar_fade_out_and_up
            }

            val listener = object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {
                    if (show) fakeToolbar.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animation) {
                    if (!show) fakeToolbar.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation) {
                    // noop
                }
            }

            AnimationUtils.loadAnimation(requireActivity(), animRes)?.let { anim ->
                anim.setAnimationListener(listener)
                fakeToolbar.startAnimation(anim)
            }
        }
    }

    private val fadeOutToolbarRunnable = Runnable {
        showToolbar(false)
    }

    override fun onImageTapped() {
        showToolbar(true)
    }

    override fun onImageLoadError() {
        uiMessageResolver.showSnack(R.string.error_loading_image)
    }

    internal inner class ImageViewerAdapter(fm: FragmentManager, val images: ArrayList<Product.Image>) :
            FragmentStatePagerAdapter(fm) {
        fun indexOfImageId(imageId: Long): Int {
            for (index in images.indices) {
                if (imageId == images[index].id) {
                    return index
                }
            }
            return -1
        }

        override fun getItem(position: Int): Fragment {
            return ImageViewerFragment.newInstance(images[position])
        }

        override fun getCount(): Int {
            return images.size
        }

        override fun setPrimaryItem(container: ViewGroup, position: Int, item: Any) {
            super.setPrimaryItem(container, position, item)
            (item as? ImageViewerFragment)?.setImageListener(this@ProductImageViewerFragment)
        }
    }
}
