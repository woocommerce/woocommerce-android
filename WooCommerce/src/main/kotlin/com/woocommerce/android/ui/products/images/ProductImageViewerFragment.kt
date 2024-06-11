package com.woocommerce.android.ui.products.images

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
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
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.products.ConfirmRemoveProductImageDialog
import com.woocommerce.android.ui.products.ImageViewerFragment
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductImageViewerFragment :
    BaseFragment(R.layout.fragment_product_image_viewer),
    ImageViewerFragment.Companion.ImageViewerListener,
    MainActivity.Companion.BackPressListener {
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    companion object {
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"
    }

    private val navArgs: ProductImageViewerFragmentArgs by navArgs()
    private val viewModel: ProductImagesViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_image_gallery)

    private var isConfirmationShowing = false
    private var confirmationDialog: AlertDialog? = null

    private var remoteMediaId = 0L
    private lateinit var pagerAdapter: ImageViewerAdapter

    private var _binding: FragmentProductImageViewerBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteMediaId = navArgs.mediaId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductImageViewerBinding.bind(view)

        setupViewPager()

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_images_title),
            onMenuItemSelected = ::onMenuItemSelected,
            onCreateMenu = ::onCreateMenu
        )

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRMATION_SHOWING)) {
                confirmRemoveProductImage()
            }
        }
    }

    private fun onCreateMenu(toolbar: Toolbar) {
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        toolbar.inflateMenu(R.menu.menu_product_delete_image)
        toolbar.menu.findItem(R.id.menu_delete_image).isVisible = navArgs.isDeletingAllowed
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.menu_delete_image -> {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_DELETE_IMAGE_BUTTON_TAPPED)
                confirmRemoveProductImage()
                true
            }

            else -> false
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
    }

    override fun onRequestAllowBackPress() = true

    private fun setupViewPager() {
        resetAdapter()

        binding.viewPager.setPageTransformer(
            MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.major_75))
        )

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
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
                    } else {
                        findNavController().navigateUp()
                    }
                }
            })
            start()
        }
    }

    override fun onImageTapped() {
        // no-op
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
