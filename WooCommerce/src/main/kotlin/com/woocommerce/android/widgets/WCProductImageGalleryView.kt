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
        private const val MAX_IMAGES_TO_PRELOAD = 4
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(imageUrl: String, sharedElement: View)
    }

    private var imageHeight = 0
    private val adapter: ImageGalleryAdapter
    private lateinit var listener: OnGalleryImageClickListener

    init {
        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false)
        itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        adapter = ImageGalleryAdapter(context).also { setAdapter(it) }
        setHasFixedSize(false)
        initPreloader()
    }

    fun showImages(images: List<WCProductImageModel>, listener: OnGalleryImageClickListener) {
        this.listener = listener
        imageHeight = this.height
        adapter.showImages(images)
    }

    /**
     * Sets up the Glide image preloader for recycler views
     * https://bumptech.github.io/glide/int/recyclerview.html
     */
    private fun initPreloader() {
        val sizeProvider = ViewPreloadSizeProvider<String>()
        val preloader = RecyclerViewPreloader<String>(Glide.with(this), this, sizeProvider, MAX_IMAGES_TO_PRELOAD)
        addOnScrollListener(preloader)
    }

    override fun getPreloadItems(position: Int): MutableList<String> {
        return Collections.singletonList(getPhotonImageUrl(position))
    }

    override fun getPreloadRequestBuilder(imageUrl: String): RequestBuilder<*>? {
        return GlideApp.with(context)
                .load(imageUrl)
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())
    }

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
            imageList.clear()
            imageList.addAll(images)
            notifyDataSetChanged()
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
            getPreloadRequestBuilder(getPhotonImageUrl(position))?.into(holder.productImage)
        }
    }

    private inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.productImage
        init {
            itemView.setOnClickListener {
                onImageClicked(adapterPosition, productImage)
            }
        }
    }
}
