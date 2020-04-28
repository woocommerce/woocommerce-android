package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.wpmedia_gallery_item.view.*
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils

/**
 * Custom recycler which displays images from the WP media library
 */
class WPMediaLibraryImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    companion object {
        private const val NUM_COLUMNS = 4
    }

    interface OnWPMediaGalleryClickListener {
        fun onWPMediaClicked(image: Product.Image, imageView: View)
    }

    private var imageSize = 0

    private val adapter: WPMediaLibraryGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: OnWPMediaGalleryClickListener

    init {
        layoutManager = GridLayoutManager(context, NUM_COLUMNS)
        itemAnimator = DefaultItemAnimator()
        layoutInflater = LayoutInflater.from(context)

        setHasFixedSize(true)
        setItemViewCacheSize(0)

        adapter = WPMediaLibraryGalleryAdapter().also {
            it.setHasStableIds(true)
            setAdapter(it)
        }

        // cancel pending Glide request when a view is recycled
        val glideRequests = GlideApp.with(this)
        setRecyclerListener { holder ->
            glideRequests.clear((holder as MPMediaViewHolder).imageView)
        }

        // create a reusable Glide request for all images
        glideRequest = glideRequests
                .asDrawable()
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())

        // create a reusable Glide rounded corner transformation for all images
        val borderRadius = context.resources.getDimensionPixelSize(R.dimen.corner_radius_small)
        glideTransform = RequestOptions.bitmapTransform(RoundedCorners(borderRadius))

        val screenWidth = DisplayUtils.getDisplayPixelWidth(context)
        val margin = context.resources.getDimensionPixelSize(R.dimen.minor_50)
        imageSize = (screenWidth / NUM_COLUMNS) - (margin * NUM_COLUMNS)
    }

    fun showImages(images: List<Product.Image>, listener: OnWPMediaGalleryClickListener) {
        this.listener = listener
        adapter.showImages(images)
    }

    private fun onImageClicked(position: Int, imageView: View) {
        listener.onWPMediaClicked(adapter.getImage(position), imageView)
    }

    private inner class WPMediaLibraryGalleryAdapter : RecyclerView.Adapter<MPMediaViewHolder>() {
        private val imageList = mutableListOf<Product.Image>()

        fun showImages(images: List<Product.Image>) {
            if (isSameImageList(images)) {
                return
            }

            imageList.clear()
            imageList.addAll(images)

            notifyDataSetChanged()
        }

        private fun isSameImageList(images: List<Product.Image>): Boolean {
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

        fun getImage(position: Int) = imageList[position]

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MPMediaViewHolder {
            return MPMediaViewHolder(
                    layoutInflater.inflate(R.layout.wpmedia_gallery_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: MPMediaViewHolder, position: Int) {
            val photonUrl = PhotonUtils.getPhotonImageUrl(getImage(position).source, 0, imageSize)
            glideRequest.load(photonUrl).apply(glideTransform).into(holder.imageView)
        }
    }

    private inner class MPMediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: BorderedImageView = view.imageView

        init {
            imageView.layoutParams.height = imageSize
            imageView.layoutParams.width = imageSize

            itemView.setOnClickListener {
                if (adapterPosition > NO_POSITION) {
                    onImageClicked(adapterPosition, imageView)
                }
            }
        }
    }
}
