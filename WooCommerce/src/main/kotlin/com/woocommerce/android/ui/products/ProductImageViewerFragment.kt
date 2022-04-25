package com.woocommerce.android.ui.products

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductImageViewerBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.products.ImageViewerFragment.Companion.ImageViewerListener
import com.woocommerce.android.util.WooAnimUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductImageViewerFragment :
    BaseFragment(R.layout.fragment_product_image_viewer),
    ImageViewerListener,
    BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    companion object {
        private const val KEY_REMOTE_MEDIA_ID = "media_id"
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"
        private const val TOOLBAR_FADE_DELAY_MS = 2500L
    }

    private val navArgs: ProductImageViewerFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by hiltNavGraphViewModels(R.id.nav_graph_image_gallery)

    private var isConfirmationShowing = false
    private var confirmationDialog: AlertDialog? = null
    private val fadeOutToolbarHandler = Handler()

    private var remoteMediaId = 0L
    private lateinit var pagerAdapter: ImageViewerAdapter

    private var _binding: FragmentProductImageViewerBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteMediaId = savedInstanceState?.getLong(KEY_REMOTE_MEDIA_ID) ?: navArgs.mediaId

        setHasOptionsMenu(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductImageViewerBinding.bind(view)

        setupViewPager()

        binding.iconBack.setOnClickListener {
            findNavController().navigateUp()
        }

        if (navArgs.isDeletingAllowed) {
            binding.iconTrash.setOnClickListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_DELETE_IMAGE_BUTTON_TAPPED)
                confirmRemoveProductImage()
            }
            binding.iconTrash.isVisible = true
        } else {
            binding.iconTrash.isVisible = false
        }

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRMATION_SHOWING)) {
                confirmRemoveProductImage()
            }
        }

        fadeOutToolbarHandler.postDelayed(fadeOutToolbarRunnable, TOOLBAR_FADE_DELAY_MS)

        @Suppress("MagicNumber")
        // If we make the Activity full-screen directly, it'll prevent the fragment transition from finishing,
        // which would prevent the previous fragment's view from getting destroyed, this causes an issue where the
        // Toolbar doesn't get restored when navigating back.
        // This seems like a bug in the fragment library.
        view.postDelayed({
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
    }

    override fun onRequestAllowBackPress() = true

    private fun setupViewPager() {
        resetAdapter()

        binding.viewPager.setPageTransformer(
            MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.margin_large))
        )

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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

        pagerAdapter = ImageViewerAdapter(this, images)
        binding.viewPager.adapter = pagerAdapter

        val position = pagerAdapter.indexOfImageId(remoteMediaId)
        if (position > -1) {
            binding.viewPager.setCurrentItem(position, false)
        }
    }

    /**
     * Confirms that the user meant to remove this image from the product
     */
    private fun confirmRemoveProductImage() {
        isConfirmationShowing = true
        confirmationDialog = ConfirmRemoveProductImageDialog(
            requireActivity(),
            onPositiveButton = this::removeCurrentImage,
            onNegativeButton = {
                isConfirmationShowing = false
            }
        ).show()
    }

    private fun removeCurrentImage() {
        val newImageCount = pagerAdapter.itemCount - 1
        val currentMediaId = remoteMediaId

        // determine the image to return to when the adapter is reloaded following the image removal
        val newMediaId = when {
            newImageCount == 0 -> {
                0
            }
            binding.viewPager.currentItem > 0 -> {
                pagerAdapter.images[binding.viewPager.currentItem - 1].id
            }
            else -> {
                pagerAdapter.images[binding.viewPager.currentItem + 1].id
            }
        }

        // animate the viewPager out, then remove the image and animate it back in - this gives
        // the appearance of the removed image being tossed away
        with(WooAnimUtils.getScaleOutAnim(binding.viewPager)) {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    remoteMediaId = newMediaId
                    viewModel.onImageRemoved(currentMediaId)
                    // animate it back in if there are any images left, others return to the previous fragment
                    if (newImageCount > 0) {
                        WooAnimUtils.scaleIn(binding.viewPager)
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
            if ((show && binding.fakeToolbar.visibility == View.VISIBLE) ||
                (!show && binding.fakeToolbar.visibility != View.VISIBLE)
            ) {
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
                    if (show) binding.fakeToolbar.visibility = View.VISIBLE
                }
                override fun onAnimationEnd(animation: Animation) {
                    if (!show) binding.fakeToolbar.visibility = View.GONE
                }
                override fun onAnimationRepeat(animation: Animation) {
                    // noop
                }
            }

            AnimationUtils.loadAnimation(requireActivity(), animRes)?.let { anim ->
                anim.setAnimationListener(listener)
                binding.fakeToolbar.startAnimation(anim)
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

    internal inner class ImageViewerAdapter(fragment: Fragment, val images: ArrayList<Product.Image>) :
        FragmentStateAdapter(fragment) {
        fun indexOfImageId(imageId: Long): Int {
            for (index in images.indices) {
                if (imageId == images[index].id) {
                    return index
                }
            }
            return -1
        }

        override fun createFragment(position: Int): Fragment {
            return ImageViewerFragment.newInstance(images[position]).also {
                it.setImageListener(this@ProductImageViewerFragment)
            }
        }

        override fun getItemCount(): Int {
            return images.size
        }
    }
}
