package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.product_list_item.view.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.util.PhotonUtils
import java.util.Collections

/**
 * Custom recycler which displays all images for a product - uses Glide's preloader for recycler views for
 * faster loading - https://bumptech.github.io/glide/int/recyclerview.html
 */
class WCProductImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    companion object {
        private const val MAX_IMAGES_TO_PRELOAD = 5
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(imageUrl: String, imageView: View)
    }

    private var imageHeight = 0
    private val adapter: ImageGalleryAdapter
    private val preloader: RecyclerViewPreloader<String>
    private val preloadSizeProvider = ViewPreloadSizeProvider<String>()
    private val request: GlideRequest<Drawable>

    private lateinit var listener: OnGalleryImageClickListener

    init {
        adapter = ImageGalleryAdapter(context).also { it.setHasStableIds(true) }
        preloader = RecyclerViewPreloader<String>(Glide.with(this),
                adapter,
                preloadSizeProvider,
                MAX_IMAGES_TO_PRELOAD)
        addOnScrollListener(preloader)

        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        itemAnimator = DefaultItemAnimator()

        setHasFixedSize(false)
        setItemViewCacheSize(0)
        setAdapter(adapter)

        val glideRequests = GlideApp.with(this)
        request = glideRequests
                .asDrawable()
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())
        setRecyclerListener { holder ->
            glideRequests.clear((holder as ImageViewHolder).imageView)
        }

        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                imageHeight = this@WCProductImageGalleryView.height
            }
        })
    }

    fun showProductImages(product: Product, listener: OnGalleryImageClickListener) {
        adapter.showImages(product.images)
        this.listener = listener
        this.visibility = if (product.images.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun onImageClicked(position: Int, imageView: View) {
        imageView.transitionName = "shared_element$position"
        listener.onGalleryImageClicked(adapter.getImageUrl(position), imageView)
    }

    private inner class ImageGalleryAdapter(private val context: Context) : RecyclerView.Adapter<ImageViewHolder>(),
            PreloadModelProvider<String> {
        private val imageList = ArrayList<WCProductImageModel>()

        fun showImages(images: List<WCProductImageModel>) {
            fun isSameImageList(): Boolean {
                if (images.size != imageList.size) {
                    return false
                }
                for (index in images.indices) {
                    if (images[index].id != imageList[index].id) {
                        return false
                    }
                }
                return true
            }

            if (!isSameImageList()) {
                imageList.clear()
                imageList.addAll(images)
                notifyDataSetChanged()
            }
        }

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            return ImageViewHolder(
                    LayoutInflater.from(context).inflate(
                            layout.image_gallery_item,
                            parent,
                            false
                    )
            ).also { preloadSizeProvider.setView(it.imageView) }
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            getPreloadRequestBuilder(getPhotonImageUrl(position)).into(holder.imageView)
        }

        /**
         * Get the actual image url for the image at the passed position
         */
        fun getImageUrl(position: Int) = adapter.imageList[position].src

        /**
         * Returns a "photon-ized" url for the image at the passed position
         */
        private fun getPhotonImageUrl(position: Int): String {
            return PhotonUtils.getPhotonImageUrl(getImageUrl(position), 0, imageHeight)
        }

        /**
         * Returns the image url at the passed position for the preloader
         */
        override fun getPreloadItems(position: Int): MutableList<String> {
            return Collections.singletonList(getPhotonImageUrl(position))
        }

        /**
         * Returns the Glide request to use for both the preloader and the adapter - must use the same Glide
         * options in both places for preloading to work
         */
        override fun getPreloadRequestBuilder(imageUrl: String) = request.load(imageUrl)
    }

    private inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.productImage
        init {
            itemView.setOnClickListener {
                onImageClicked(adapterPosition, imageView)
            }
        }
    }
}
