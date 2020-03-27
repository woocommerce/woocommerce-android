package com.woocommerce.android.ui.products

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.imageviewer.ImageViewerFragment
import com.woocommerce.android.ui.imageviewer.ImageViewerFragment.Companion.ImageViewerListener
import kotlinx.android.synthetic.main.fragment_product_image_viewer.*

class ProductImageViewerFragment : BaseProductFragment(), ImageViewerListener {
    companion object {
        private const val TOOLBAR_FADE_DELAY_MS = 2500L
    }

    private var remoteMediaId = 0L
    private lateinit var pagerAdapter: ImageViewerAdapter

    private val fadeOutToolbarHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showToolbar(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_image_viewer, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getProduct().productDraft?.let {
            setupViewPager(it.images)
        }
    }

    override fun onRequestAllowBackPress() = true

    override fun onImageTapped() {
        showToolbar(true)
    }

    override fun onImageLoadError() {
        uiMessageResolver.showSnack(R.string.error_loading_image)
    }

    private fun showToolbar(show: Boolean) {
        if (!isAdded) return

        requireActivity().findViewById<Toolbar>(R.id.toolbar)?.let { toolbar ->
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

            AnimationUtils.loadAnimation(requireActivity(), animRes)?.let { anim ->
                anim.setAnimationListener(listener)
                toolbar.startAnimation(anim)
            }
        }
    }

    private val fadeOutToolbarRunnable = Runnable {
        showToolbar(false)
    }

    private fun setupViewPager(images: List<Product.Image>) {
        pagerAdapter = ImageViewerAdapter(requireActivity().supportFragmentManager, images)
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
