package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.navArgs
import androidx.transition.ChangeBounds
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ImageViewerFragment.Companion.ImageViewerListener
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

        setHasOptionsMenu(true)
        sharedElementEnterTransition = ChangeBounds()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_image_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupViewPager()

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_trash, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_trash -> {
                confirmRemoveProductImage()
                true
            } else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onRequestAllowBackPress() = true

    override fun onImageLoadError() {
        uiMessageResolver.showSnack(R.string.error_loading_image)
    }

    private fun setupViewPager() {
        pagerAdapter = ImageViewerAdapter(requireActivity().supportFragmentManager).also {
            it.loadProductImages()
        }
        viewPager.adapter = pagerAdapter
        viewPager.pageMargin = resources.getDimensionPixelSize(R.dimen.margin_large)

        // determine the position of the original media item so we can page to it immediately
        val position = pagerAdapter.indexOfImageId(remoteMediaId)
        if (position > -1) {
            viewPager.currentItem = position
        }

        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // remember this image id so we can return to it upon rotation, and so
                // we use the right image if the user requests to remove it
                remoteMediaId = pagerAdapter.images[position].id
            }
        })
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
                    viewModel.removeProductImageFromDraft(remoteMediaId)
                    pagerAdapter.loadProductImages()
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    isConfirmationShowing = false
                }
                .show()
    }

    internal inner class ImageViewerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        val images = ArrayList<Product.Image>()

        fun loadProductImages() {
            images.clear()
            viewModel.getProduct().productDraft?.let { product ->
                images.addAll(product.images)
            }
            notifyDataSetChanged()
        }

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
