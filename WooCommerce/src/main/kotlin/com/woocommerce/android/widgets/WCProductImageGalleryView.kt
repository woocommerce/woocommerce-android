package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_CROP
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.product_list_item.view.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.util.PhotonUtils

/**
 * Custom recycler which displays all images for a product
 */
class WCProductImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(imageUrl: String, imageView: View)
    }

    private var imageHeight = 0
    private val orientation: Int

    private val adapter: ImageGalleryAdapter
    private val request: GlideRequest<Drawable>
    private val layoutInflater: LayoutInflater

    private lateinit var listener: OnGalleryImageClickListener

    init {
        orientation = attrs?.let {
            val attributeArray = context.obtainStyledAttributes(it, R.styleable.WCProductImageGalleryView)
            attributeArray.getInt(R.styleable.WCProductImageGalleryView_galleryOrientation, VERTICAL)
        } ?: VERTICAL

        layoutManager = LinearLayoutManager(context, orientation, false)
        itemAnimator = DefaultItemAnimator()
        layoutInflater = LayoutInflater.from(context)

        setHasFixedSize(false)
        setItemViewCacheSize(0)

        adapter = ImageGalleryAdapter(context).also {
            it.setHasStableIds(true)
            setAdapter(it)
        }

        // cancel pending Glide request when a view is recycled
        val glideRequests = GlideApp.with(this)
        setRecyclerListener { holder ->
            glideRequests.clear((holder as ImageViewHolder).imageView)
        }

        // create a reusable Glide request for all images
        request = glideRequests
                .asDrawable()
                .error(R.drawable.ic_product)
                .placeholder(R.drawable.product_detail_image_background)
                .transition(DrawableTransitionOptions.withCrossFade())

        // make images fit the entire height of the view
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = this@WCProductImageGalleryView.height
                val width = this@WCProductImageGalleryView.width
                imageHeight = if (orientation == HORIZONTAL) height else height / 3
            }
        })
    }

    fun showProductImages(product: Product, listener: OnGalleryImageClickListener) {
        this.listener = listener
        this.visibility = if (product.images.isNotEmpty()) View.VISIBLE else View.GONE
        adapter.showImages(product.images)
    }

    private fun onImageClicked(position: Int, imageView: View) {
        imageView.transitionName = "shared_element$position"
        listener.onGalleryImageClicked(adapter.getImageUrl(position), imageView)
    }

    private inner class ImageGalleryAdapter(private val context: Context) : RecyclerView.Adapter<ImageViewHolder>() {
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

        fun getImageUrl(position: Int) = adapter.imageList[position].src

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            return ImageViewHolder(
                    layoutInflater.inflate(
                            layout.image_gallery_item,
                            parent,
                            false
                    )
            )
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val photonUrl = PhotonUtils.getPhotonImageUrl(getImageUrl(position), 0, imageHeight)
            request.load(photonUrl).into(holder.imageView)
        }
    }

    private inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.productImage
        init {
            if (orientation == VERTICAL) {
                imageView.scaleType = CENTER_CROP
            }
            imageView.layoutParams.height = imageHeight
            itemView.setOnClickListener {
                onImageClicked(adapterPosition, imageView)
            }
        }
    }
}
