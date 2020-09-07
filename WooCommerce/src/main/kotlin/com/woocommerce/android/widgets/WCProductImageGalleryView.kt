package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ProgressBar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import kotlinx.android.synthetic.main.image_gallery_item.view.*
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import java.util.Date

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
        private const val VIEW_TYPE_ADD_IMAGE = 2
        private const val NUM_COLUMNS = 2
        private const val ADD_IMAGE_ITEM_ID = Long.MAX_VALUE
    }

    interface OnGalleryImageClickListener {
        fun onGalleryImageClicked(image: Product.Image)
        fun onGalleryAddImageClicked() { }
    }

    private var imageSize = 0
    private var isGridView = false
    private var showAddImageIcon = false

    private val adapter: ImageGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: OnGalleryImageClickListener

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCProductImageGalleryView)
            try {
                isGridView = attrArray.getBoolean(R.styleable.WCProductImageGalleryView_isGridView, false)
                showAddImageIcon = attrArray.getBoolean(
                        R.styleable.WCProductImageGalleryView_showAddImageIcon,
                        false
                )
            } finally {
                attrArray.recycle()
            }
        }

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
            glideRequests.clear((holder as ImageViewHolder).productImageView)
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

        imageSize = if (isGridView) {
            val screenWidth = DisplayUtils.getDisplayPixelWidth(context)
            val margin = context.resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            (screenWidth / 2) - (margin * 2)
        } else {
            context.resources.getDimensionPixelSize(R.dimen.image_major_120)
        }
    }

    fun showProductImages(images: List<Product.Image>, listener: OnGalleryImageClickListener) {
        this.listener = listener
        adapter.showImages(images)
    }

    /**
     * Show upload placeholders for the passed local image Uris
     */
    fun setPlaceholderImageUris(imageUriList: List<Uri>?) {
        if (imageUriList.isNullOrEmpty()) {
            if (adapter.clearPlaceholders()) {
                adapter.notifyDataSetChanged()
            }
        } else {
            val placeholders = ArrayList<Product.Image>()

            for (index in imageUriList.indices) {
                // use a negative id so we can check it in isPlaceholder() below
                val id = (-index - 1).toLong()
                // set the image src to this uri so we can preview it while uploading
                placeholders.add(0, Product.Image(id, "", imageUriList[index].toString(), Date()))
            }

            adapter.setPlaceholderImages(placeholders)
        }
    }

    private fun onImageClicked(position: Int) {
        val viewType = adapter.getItemViewType(position)
        if (viewType == VIEW_TYPE_IMAGE) {
            listener.onGalleryImageClicked(adapter.getImage(position))
        } else if (viewType == VIEW_TYPE_ADD_IMAGE) {
            listener.onGalleryAddImageClicked()
        }
    }

    private inner class ImageGalleryAdapter : RecyclerView.Adapter<ImageViewHolder>() {
        private val imageList = mutableListOf<Product.Image>()

        fun showImages(images: List<Product.Image>) {
            if (isSameImageList(images)) {
                return
            }

            val placeholders = getPlaceholderImages()

            imageList.clear()
            imageList.addAll(images)

            // restore the "Add image" icon (never shown when list is empty)
            if (showAddImageIcon && imageList.size > 0) {
                imageList.add(Product.Image(
                        id = ADD_IMAGE_ITEM_ID,
                        name = "",
                        source = "",
                        dateCreated = Date()))
            }

            notifyDataSetChanged()

            if (placeholders.isNotEmpty()) {
                setPlaceholderImages(placeholders)
            }
        }

        /**
         * Returns the list of images without placeholders or the "add image" icon
         */
        private fun getActualImages(): List<Product.Image> {
            return imageList.filterIndexed { index, _ -> getItemViewType(index) == VIEW_TYPE_IMAGE }
        }

        /**
         * Returns the list of placeholder images
         */
        private fun getPlaceholderImages(): List<Product.Image> {
            return imageList.filterIndexed { index, _ -> isPlaceholder(index) }
        }

        /**
         * Returns true if the passed list of images is the same as the adapter's list, taking
         * placeholders into account
         */
        private fun isSameImageList(images: List<Product.Image>): Boolean {
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

        fun setPlaceholderImages(placeholders: List<Product.Image>) {
            // remove existing placeholders
            var didChange = clearPlaceholders()

            // add the new ones to the top of the list
            if (placeholders.isNotEmpty()) {
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
        fun clearPlaceholders(): Boolean {
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
                showAddImageIcon && imageList[position].id == ADD_IMAGE_ITEM_ID -> VIEW_TYPE_ADD_IMAGE
                isPlaceholder(position) -> VIEW_TYPE_PLACEHOLDER
                else -> VIEW_TYPE_IMAGE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                    layoutInflater.inflate(R.layout.image_gallery_item, parent, false)
            )

            when (viewType) {
                VIEW_TYPE_PLACEHOLDER -> {
                    holder.productImageView.visibility = View.VISIBLE
                    holder.productImageView.alpha = 0.5F
                    holder.uploadProgress.visibility = View.VISIBLE
                    holder.addImageContainer.visibility = View.GONE
                }
                VIEW_TYPE_ADD_IMAGE -> {
                    holder.productImageView.visibility = View.GONE
                    holder.uploadProgress.visibility = View.GONE
                    holder.addImageContainer.visibility = View.VISIBLE
                }
                else -> {
                    holder.productImageView.visibility = View.VISIBLE
                    holder.productImageView.alpha = 1.0F
                    holder.uploadProgress.visibility = View.GONE
                    holder.addImageContainer.visibility = View.GONE
                }
            }

            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val src = getImage(position).source
            val viewType = getItemViewType(position)
            if (viewType == VIEW_TYPE_PLACEHOLDER) {
                glideRequest.load(Uri.parse(src)).apply(glideTransform).into(holder.productImageView)
            } else if (viewType == VIEW_TYPE_IMAGE) {
                val photonUrl = PhotonUtils.getPhotonImageUrl(src, 0, imageSize)
                glideRequest.load(photonUrl).apply(glideTransform).into(holder.productImageView)
            }
        }
    }

    private inner class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImageView: BorderedImageView = view.productImage
        val uploadProgress: ProgressBar = view.uploadProgess
        val addImageContainer: ViewGroup = view.addImageContainer

        init {
            productImageView.layoutParams.height = imageSize
            productImageView.layoutParams.width = if (isGridView) imageSize else WRAP_CONTENT

            addImageContainer.layoutParams.height = imageSize
            addImageContainer.layoutParams.width = imageSize

            // add space between items in grid view
            if (isGridView) {
                val margin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
                with(productImageView.layoutParams as MarginLayoutParams) {
                    this.topMargin = margin
                    this.bottomMargin = margin
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition > NO_POSITION) {
                    onImageClicked(adapterPosition)
                }
            }
        }
    }
}
