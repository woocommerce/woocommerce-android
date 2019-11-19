package com.woocommerce.android.widgets

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
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
    private val layoutInflater: LayoutInflater
    private val contentResolver: ContentResolver

    private val request: GlideRequest<Drawable>

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
        contentResolver = context.contentResolver

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
        adapter.showImages(product.images)
    }

    /**
     * Show upload placeholders for the passed local image Uris
     */
    fun setPlaceholderImageUris(imageUriList: List<Uri>) {
        val placeholders = ArrayList<WCProductImageModel>()
        for (index in imageUriList.indices) {
            // use a negative id so we can check it in isPlaceholder() below
            val id = (-index - 1).toLong()
            placeholders.add(0, WCProductImageModel(id).also {
                // set the image src to this uri so we can preview it while uploading
                it.src = imageUriList[index].toString()
            })
        }
        adapter.setPlaceholderImages(placeholders)
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
            if (isSameImageList(images)) {
                return
            }

            val placeholders = getPlaceholderImages()

            imageList.clear()
            imageList.addAll(images)
            notifyDataSetChanged()

            if (placeholders.size > 0) {
                setPlaceholderImages(placeholders)
            }
        }

        /**
         * Returns the list of images without placeholders
         */
        private fun getActualImages(): List<WCProductImageModel> {
            val images = ArrayList<WCProductImageModel>()
            for (index in imageList.indices) {
                if (!isPlaceholder(index)) {
                    images.add(imageList[index])
                }
            }
            return images
        }

        /**
         * Returns the list of placeholder images
         */
        private fun getPlaceholderImages(): List<WCProductImageModel> {
            val images = ArrayList<WCProductImageModel>()
            for (index in imageList.indices) {
                if (isPlaceholder(index)) {
                    images.add(imageList[index])
                }
            }
            return images
        }

        /**
         * Returns true if the passed list of images is the same as the adapter's list, taking
         * placeholders into account
         */
        private fun isSameImageList(images: List<WCProductImageModel>): Boolean {
            val actualImages = getActualImages()
            if (images.size != actualImages.size) {
                return false
            }

            for (index in images.indices) {
                if (images[index].id != actualImages[index].id) {
                    return false
                }
            }
            return true
        }

        fun setPlaceholderImages(placeholders: List<WCProductImageModel>) {
            // remove existing placeholders
           var didChange = clearPlaceholders()

            // add the new ones to the top of the list
            if (placeholders.size > 0) {
                imageList.addAll(0, placeholders)
                didChange = true
            }

            if (didChange) {
                notifyDataSetChanged()
            }
        }

        /**
         * Removes all placeholders, returns true only if any were removed
         */
        private fun clearPlaceholders(): Boolean {
            var result = false
            while (itemCount > 0 && isPlaceholder(0)) {
                imageList.removeAt(0)
                result = true
            }
            return result
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
                holder.imageView.alpha = 0.5F
                holder.uploadProgress.visibility = View.VISIBLE
            } else {
                holder.imageView.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                holder.imageView.alpha = 1.0F
                holder.uploadProgress.visibility = View.GONE
            }

            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val src = getImage(position).src
            if (getItemViewType(position) == VIEW_TYPE_PLACEHOLDER) {
                request.load(Uri.parse(src)).into(holder.imageView)
            } else {
                val photonUrl = PhotonUtils.getPhotonImageUrl(src, 0, imageHeight)
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
