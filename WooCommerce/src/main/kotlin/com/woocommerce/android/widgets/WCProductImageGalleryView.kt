package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.image_gallery_item.view.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils

/**
 * Custom recycler which displays all images for a product
 */
class WCProductImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_PLACEHOLDER = 1
        private const val NUM_COLUMNS = 2
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(imageModel: WCProductImageModel, imageView: View)
    }

    private var imageHeight = 0
    private var isGridView = false

    private val placeholderWidth: Int
    private val adapter: ImageGalleryAdapter
    private val request: GlideRequest<Drawable>
    private val layoutInflater: LayoutInflater

    private lateinit var listener: OnGalleryImageClickListener

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCProductImageGalleryView)
            try {
                isGridView = attrArray.getBoolean(R.styleable.WCProductImageGalleryView_isGridView, false)
            } finally {
                attrArray.recycle()
            }
        }

        placeholderWidth = DisplayUtils.getDisplayPixelWidth(context) / NUM_COLUMNS

        layoutManager = if (isGridView) {
            GridLayoutManager(context, NUM_COLUMNS)
        } else {
            LinearLayoutManager(context, HORIZONTAL, false)
        }

        itemAnimator = DefaultItemAnimator()
        layoutInflater = LayoutInflater.from(context)

        setHasFixedSize(false)
        setItemViewCacheSize(0)

        adapter = ImageGalleryAdapter().also {
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

        imageHeight = if (isGridView) {
            context.resources.getDimensionPixelSize(R.dimen.product_image_gallery_image_height_grid)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.product_image_gallery_image_height)
        }
    }

    fun showProductImages(product: Product, listener: OnGalleryImageClickListener) {
        this.listener = listener

        if (!adapter.isSameImageList(product.images)) {
            adapter.showImages(product.images)
        }
    }

    /**
     * Set the number of upload placeholders to show
     */
    fun setPlaceholderCount(count: Int) {
        adapter.setPlaceholderCount(count)
    }

    private fun onImageClicked(position: Int, imageView: View) {
        if (!adapter.isPlaceholder(position)) {
            imageView.transitionName = "shared_element$position"
            listener.onGalleryImageClicked(adapter.getImage(position), imageView)
        }
    }

    private inner class ImageGalleryAdapter : RecyclerView.Adapter<ImageViewHolder>() {
        private val imageList = ArrayList<WCProductImageModel>()
        private var placeholderCount = 0

        fun showImages(images: List<WCProductImageModel>) {
            val count = placeholderCount

            imageList.clear()
            imageList.addAll(images)
            notifyDataSetChanged()

            setPlaceholderCount(count)
        }

        fun isSameImageList(images: List<WCProductImageModel>): Boolean {
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

        fun setPlaceholderCount(count: Int) {
            if (count == placeholderCount) {
                return
            }

            // remove existing placeholders
            for (index in 1..placeholderCount) {
                imageList.removeAt(index)
            }

            // add the new ones
            for (index in 1..count) {
                // use a negative id so we can check it in isPlaceholder() below
                val id = -index.toLong()
                imageList.add(0, WCProductImageModel(id))
            }

            placeholderCount = count
            notifyDataSetChanged()
        }

        fun isPlaceholder(position: Int) = imageList[position].id < 0

        fun getImage(position: Int) = imageList[position]

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun getItemViewType(position: Int): Int {
            return when {
                isPlaceholder(position) -> VIEW_TYPE_PLACEHOLDER
                else -> VIEW_TYPE_IMAGE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                    layoutInflater.inflate(R.layout.image_gallery_item, parent, false)
            )

            if (viewType == VIEW_TYPE_PLACEHOLDER) {
                holder.imageView.layoutParams.width = placeholderWidth
                holder.uploadProgress.visibility = View.VISIBLE
            } else {
                holder.imageView.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                holder.uploadProgress.visibility = View.GONE
            }

            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            if (getItemViewType(position) == VIEW_TYPE_IMAGE) {
                val photonUrl = PhotonUtils.getPhotonImageUrl(getImage(position).src, 0, imageHeight)
                request.load(photonUrl).into(holder.imageView)
            }
        }
    }

    private inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.productImage
        val uploadProgress: ProgressBar = view.uploadProgess
        init {
            imageView.layoutParams.height = imageHeight
            itemView.setOnClickListener {
                onImageClicked(adapterPosition, imageView)
            }
        }
    }
}
