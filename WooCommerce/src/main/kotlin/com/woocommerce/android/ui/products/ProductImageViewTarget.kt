package com.woocommerce.android.ui.products

import android.view.View
import android.widget.ImageView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.extensions.loadPhotonUrlWithFallback

class ProductImageViewTarget(
    private val imageView: ImageView,
    private val factory: ProductImageLoader.Factory,
    private val imageSize: Int = imageView.resources.getDimensionPixelSize(R.dimen.image_minor_100),
    private val cornerRadius: Int = imageView.resources.getDimensionPixelSize(R.dimen.corner_radius_image),
) : ProductImageTarget, View.OnAttachStateChangeListener {
    private var imageLoader: ProductImageLoader? = null
    private var remoteProductId: Long? = null

    init {
        imageView.addOnAttachStateChangeListener(this)
    }

    fun load(remoteProductId: Long) {
        this.remoteProductId = remoteProductId
        when (imageView.isAttachedToWindow) {
            true -> loadImage(remoteProductId)
            false -> show(null)
        }
    }

    override fun show(imageUrl: String?) {
        val request = Glide.with(imageView)
            .asDrawable()
            .placeholder(R.drawable.ic_product)
        when {
            cornerRadius > 0 -> request.transform(CenterCrop(), RoundedCorners(cornerRadius))
            else -> request
        }.loadPhotonUrlWithFallback(imageUrl, imageSize, imageSize)
            .into(imageView)
    }

    override fun onViewAttachedToWindow(view: View) {
        remoteProductId?.let { loadImage(it) }
    }

    override fun onViewDetachedFromWindow(view: View) {
        imageLoader?.cancel()
        imageLoader = null
    }

    private fun loadImage(remoteProductId: Long) {
        val loader = imageLoader ?: factory.create(
            target = this,
            scope = checkNotNull(imageView.findViewTreeLifecycleOwner()).lifecycleScope
        ).also { imageLoader = it }
        loader.load(remoteProductId)
    }
}
