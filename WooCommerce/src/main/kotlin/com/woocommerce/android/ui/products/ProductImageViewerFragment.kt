package com.woocommerce.android.ui.products

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ImageViewerFragment.Companion.ImageViewerListener
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooAnimUtils
import kotlinx.android.synthetic.main.fragment_product_image_viewer.*

class ProductImageViewerFragment : BaseProductFragment(), ImageViewerListener {
    companion object {
        private const val KEY_REMOTE_MEDIA_ID = "media_id"
        private const val KEY_IS_CONFIRMATION_SHOWING = "is_confirmation_showing"
    }

    private val navArgs: ProductImageViewerFragmentArgs by navArgs()

    private var isConfirmationShowing = false
    private var confirmationDialog: AlertDialog? = null

    private var remoteMediaId = 0L
    private lateinit var pagerAdapter: ImageViewerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        remoteMediaId = savedInstanceState?.getLong(KEY_REMOTE_MEDIA_ID) ?: navArgs.remoteMediaId

        setHasOptionsMenu(false)
        sharedElementEnterTransition = ChangeBounds()
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

        if (FeatureFlag.PRODUCT_RELEASE_M2.isEnabled()) {
            iconTrash.setOnClickListener {
                confirmRemoveProductImage()
            }
        } else {
            iconTrash.visibility = View.GONE
        }

        savedInstanceState?.let { bundle ->
            if (bundle.getBoolean(KEY_IS_CONFIRMATION_SHOWING)) {
                confirmRemoveProductImage()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CONFIRMATION_SHOWING, isConfirmationShowing)
    }

    override fun onRequestAllowBackPress() = true

    override fun onImageLoadError() {
        uiMessageResolver.showSnack(R.string.error_loading_image)
    }

    private fun setupViewPager() {
        resetAdapter()

        viewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_large)

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // remember this image id so we can return to it upon rotation, and so
                // we use the right image if the user requests to remove it
                remoteMediaId = pagerAdapter.images[position].id
            }
        })
    }

    private fun resetAdapter() {
        val images = ArrayList<Product.Image>()
        viewModel.getProduct().productDraft?.let { draft ->
            images.addAll(draft.images)
        }

        pagerAdapter = ImageViewerAdapter(requireActivity().supportFragmentManager, images)
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
        confirmationDialog = AlertDialog.Builder(ContextThemeWrapper(requireActivity(), R.style.AppTheme))
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
                    viewModel.removeProductImageFromDraft(currentMediaId)
                    // animate it back in if there are any images left, others return to the previous fragment
                    if (newImageCount > 0) {
                        WooAnimUtils.scaleIn(viewPager)
                        resetAdapter()
                    } else {
                        findNavController().navigateUp()
                    }
                }
            })
            start()
        }
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
    }
}
