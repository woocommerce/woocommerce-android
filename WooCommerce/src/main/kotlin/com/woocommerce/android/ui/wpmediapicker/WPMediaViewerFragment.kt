package com.woocommerce.android.ui.wpmediapicker

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentWpmediaViewerBinding
import com.woocommerce.android.di.GlideApp

/**
 * Fullscreen single image viewer
 */
class WPMediaViewerFragment : androidx.fragment.app.Fragment(R.layout.fragment_wpmedia_viewer),
    RequestListener<Drawable> {
    private val navArgs: WPMediaViewerFragmentArgs by navArgs()

    private var _binding: FragmentWpmediaViewerBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentWpmediaViewerBinding.bind(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.iconBack.setOnClickListener {
            findNavController().navigateUp()
        }
        loadImage()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun loadImage() {
        binding.progressBar.isVisible = true
        GlideApp.with(this)
                .load(navArgs.imageUrl)
                .listener(this)
                .into(binding.photoView)
    }

    /**
     * Glide failed to load the image
     */
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        isFirstResource: Boolean
    ): Boolean {
        binding.progressBar.isVisible = false
        return false
    }

    /**
     * Glide has loaded the image, hide the progress bar
     */
    override fun onResourceReady(
        resource: Drawable?,
        model: Any?,
        target: com.bumptech.glide.request.target.Target<Drawable>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean {
        binding.progressBar.isVisible = false
        return false
    }
}
