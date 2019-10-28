package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader.PreloadModelProvider
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.util.ViewPreloadSizeProvider
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.product_list_item.view.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.util.PhotonUtils
import java.util.Collections

class WCProductImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle), PreloadModelProvider<String> {
    companion object {
        private const val MAX_IMAGES_TO_PRELOAD = 10
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(imageUrl: String, sharedElement: View)
    }

    private var imageHeight = 0
    private val adapter: ImageGalleryAdapter
    private val preloader: RecyclerViewPreloader<String>
    private lateinit var listener: OnGalleryImageClickListener

    init {
        // Set up the Glide preloader for recycler views - https://bumptech.github.io/glide/int/recyclerview.html
        preloader = RecyclerViewPreloader<String>(Glide.with(this),
                this,
                ViewPreloadSizeProvider<String>(),
                MAX_IMAGES_TO_PRELOAD)
        addOnScrollListener(preloader)

        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false)
        itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        adapter = ImageGalleryAdapter(context).also { setAdapter(it) }
        setHasFixedSize(false)
    }

    fun showImages(images: List<WCProductImageModel>, listener: OnGalleryImageClickListener) {
        this.listener = listener
        imageHeight = this.height
        adapter.showImages(images)
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
    override fun getPreloadRequestBuilder(imageUrl: String): RequestBuilder<*>? {
        return GlideApp.with(context)
                .load(imageUrl)
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())
    }

    /**
     * Returns a "photon-ized" url for the image at the passed position
     */
    private fun getPhotonImageUrl(position: Int): String {
        val imageUrl = adapter.imageList[position].src
        return PhotonUtils.getPhotonImageUrl(imageUrl, 0, imageHeight)
    }

    private fun onImageClicked(position: Int, sharedElement: View) {
        sharedElement.transitionName = "shared_element$position"
        val imageUrl = adapter.imageList[position].src
        listener.onGalleryImageClicked(imageUrl, sharedElement)
    }

    private inner class ImageGalleryAdapter(private val context: Context) : RecyclerView.Adapter<ImageViewHolder>() {
        val imageList = ArrayList<WCProductImageModel>()

        fun showImages(images: List<WCProductImageModel>) {
            if (!isSameImageList(images)) {
                imageList.clear()
                imageList.addAll(images)
                notifyDataSetChanged()
            }
        }

        private fun isSameImageList(images: List<WCProductImageModel>): Boolean {
            if (images.size != imageList.size) {
                return false
            }
            for (i in 0 until images.size) {
                if (images[i].id != imageList[i].id) {
                    return false
                }
            }
            return true
        }

        override fun getItemCount(): Int {
            return imageList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            return ImageViewHolder(
                    LayoutInflater.from(context).inflate(
                            layout.image_gallery_item,
                            parent,
                            false
                    )
            )
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            getPreloadRequestBuilder(getPhotonImageUrl(position))?.into(holder.imageView)
        }
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
