package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
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
        private const val PLACEHOLDER_ID = -1L
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_PLACEHOLDER = 1
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(image: WCProductImageModel, imageView: View)
    }

    private var imageHeight = 0
    private var isGridView = false

    private val numColumns: Int
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

        numColumns = if (DisplayUtils.isLandscape(context)) 3 else 2
        placeholderWidth = DisplayUtils.getDisplayPixelWidth(context) / numColumns

        layoutManager = if (isGridView) {
            GridLayoutManager(context, numColumns)
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

        // if this is showing a grid make images a percentage of the view's height, otherwise make
        // images fit the entire height of the view
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                val height = this@WCProductImageGalleryView.height
                imageHeight = if (isGridView) height / 3 else height
            }
        })
    }

    fun showProductImages(product: Product, listener: OnGalleryImageClickListener) {
        this.listener = listener
        this.visibility = if (product.images.isNotEmpty()) View.VISIBLE else View.GONE

        // if the imageHeight is already known show the images immediately, otherwise invalidate the view
        // so the imageHeight can be determined and then show the images after a brief delay
        if (imageHeight > 0) {
            adapter.showImages(product.images)
        } else {
            invalidate()
            postDelayed({
                adapter.showImages(product.images)
            }, 100)
        }
    }

    fun addUploadPlaceholder() {
        adapter.addUploadPlaceholder()
        smoothScrollToPosition(0)
    }

    fun removeUploadPlaceholder() {
        adapter.removeUploadPlaceholder()
    }

    private fun onImageClicked(position: Int, imageView: View) {
        if (!adapter.isPlaceholder(position)) {
            imageView.transitionName = "shared_element$position"
            listener.onGalleryImageClicked(adapter.getImage(position), imageView)
        }
    }

    private inner class ImageGalleryAdapter : RecyclerView.Adapter<ImageViewHolder>() {
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

        fun addUploadPlaceholder() {
            removeUploadPlaceholder()
            val placeholder = WCProductImageModel(PLACEHOLDER_ID)
            imageList.add(0, placeholder)
            notifyItemInserted(0)
        }

        fun removeUploadPlaceholder() {
            for (index in imageList.indices) {
                if (isPlaceholder(index)) {
                    imageList.removeAt(index)
                    notifyItemRemoved(index)
                    return
                }
            }
        }

        fun isPlaceholder(position: Int) = imageList[position].id == PLACEHOLDER_ID

        fun getImage(position: Int) = imageList[position]

        override fun getItemCount() = imageList.size

        override fun getItemId(position: Int): Long = imageList[position].id

        override fun getItemViewType(position: Int) =
                if (isPlaceholder(position)) VIEW_TYPE_PLACEHOLDER else VIEW_TYPE_IMAGE

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                    layoutInflater.inflate(R.layout.image_gallery_item, parent, false)
            )

            if (viewType == VIEW_TYPE_PLACEHOLDER) {
                holder.imageView.layoutParams.width = placeholderWidth
                holder.imageView.setBackgroundResource(R.drawable.product_detail_image_background)
                holder.uploadProgress.visibility = View.VISIBLE
            } else {
                holder.imageView.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                holder.imageView.setBackgroundResource(R.drawable.picture_frame)
                holder.uploadProgress.visibility = View.GONE
            }

            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            if (!isPlaceholder(position)) {
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
