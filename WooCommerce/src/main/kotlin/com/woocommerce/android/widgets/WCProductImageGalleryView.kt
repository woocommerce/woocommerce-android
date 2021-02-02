package com.woocommerce.android.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ImageGalleryItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
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
        private const val NUM_GRID_MARGINS = 3
    }

    interface OnGalleryImageInteractionListener {
        fun onGalleryImageClicked(image: Product.Image)
        fun onGalleryAddImageClicked() { }
        fun onGalleryImageDragStarted() { }
        fun onGalleryImageMoved(from: Int, to: Int) { }
        fun onGalleryImageDeleteIconClicked(image: Product.Image) { }
    }

    private var imageSize = 0
    private var isGridView = false
    private var showAddImageIcon = false
    private var isDraggingEnabled = false

    private val adapter: ImageGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: OnGalleryImageInteractionListener

    private val draggableItemTouchHelper = DraggableItemTouchHelper(
            dragDirs = ItemTouchHelper.START or
                    ItemTouchHelper.END or
                    ItemTouchHelper.UP or
                    ItemTouchHelper.DOWN,
            onDragStarted = {
                listener.onGalleryImageDragStarted()
            },
            onMove = this::onProductImagesPositionChanged
    )

    init {
        attrs?.let {
            val attrArray = context.obtainStyledAttributes(it, R.styleable.WCProductImageGalleryView)
            try {
                isGridView = attrArray.getBoolean(R.styleable.WCProductImageGalleryView_isGridView, false)
                showAddImageIcon = attrArray.getBoolean(
                        R.styleable.WCProductImageGalleryView_showAddImageIcon,
                        false
                )
                isDraggingEnabled = attrArray.getBoolean(R.styleable.WCProductImageGalleryView_isDraggingEnabled, false)
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
            glideRequests.clear((holder as ImageViewHolder).viewBinding.productImage)
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
            val deleteIconsSpace = context.resources.getDimensionPixelSize(R.dimen.margin_extra_large)
            ((screenWidth - margin * NUM_GRID_MARGINS) / 2) - deleteIconsSpace
        } else {
            context.resources.getDimensionPixelSize(R.dimen.image_major_120)
        }

        addItemDecoration(
                if (isGridView) {
                    GridItemDecoration(
                            spanCount = NUM_COLUMNS,
                            spacing = resources.getDimensionPixelSize(R.dimen.margin_extra_large)
                    )
                } else {
                    HorizontalItemDecoration(
                            spacing = resources.getDimensionPixelSize(R.dimen.minor_100)
                    )
                }
        )
    }

    fun showProductImages(
        images: List<Product.Image>,
        listener: OnGalleryImageInteractionListener
    ) {
        this.listener = listener
        adapter.showImages(images)

        updateDraggingItemTouchHelper(images)
    }

    fun showProductImage(image: Product.Image, listener: OnGalleryImageInteractionListener) {
        showProductImages(listOf(image), listener)
    }

    private fun updateDraggingItemTouchHelper(images: List<Product.Image>) {
        draggableItemTouchHelper.attachToRecyclerView(
                if (isDraggingEnabled && images.size > 1) this else null
        )
    }

    private fun onProductImagesPositionChanged(from: Int, to: Int) {
        listener.onGalleryImageMoved(from, to)
    }

    fun clearImages() {
        adapter.clearImages()
    }

    fun clearPlaceholders() {
        adapter.clearPlaceholders()
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

    fun setDraggingState(isDragging: Boolean) {
        adapter.setDraggingState(isDragging)
    }

    private inner class ImageGalleryAdapter : RecyclerView.Adapter<ImageViewHolder>() {
        private val imageList = mutableListOf<Product.Image>()

        val isDragging = MutableLiveData<Boolean>(false)

        fun clearImages() {
            imageList.clear()
            notifyDataSetChanged()
        }

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

        fun setDraggingState(isDragging: Boolean) {
            this.isDragging.value = isDragging
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
                val list = placeholders + imageList
                imageList.clear()
                imageList.addAll(list)
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

        override fun onViewAttachedToWindow(holder: ImageViewHolder) {
            holder.onViewAttached()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                ImageGalleryItemBinding.inflate(layoutInflater, parent, false),
                isDragging
            )

            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val image = getImage(position)
            val viewType = getItemViewType(position)
            holder.bind(image, viewType)
        }

        override fun onViewDetachedFromWindow(holder: ImageViewHolder) {
            holder.onViewDetached()
        }
    }

    private inner class ImageViewHolder(
        val viewBinding: ImageGalleryItemBinding,
        private val isDraggingEnabled: LiveData<Boolean>
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.productImage.layoutParams.height = imageSize
            viewBinding.productImage.layoutParams.width = if (isGridView) imageSize else WRAP_CONTENT

            viewBinding.addImageContainer.layoutParams.height = imageSize
            viewBinding.addImageContainer.layoutParams.width = imageSize

            setMargins()
        }
        @SuppressLint("ClickableViewAccessibility")
        private val dragOnTouchListener = OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && draggableItemTouchHelper.isAttached) {
                draggableItemTouchHelper.startDrag(this@ImageViewHolder)
            }
            return@OnTouchListener false
        }

        private val onClickListener = OnClickListener {
            if (adapterPosition > NO_POSITION) {
                onImageClicked(adapterPosition)
            }
        }

        private val onDraggingEnabledChanged: (Boolean) -> Unit = { enabled ->
            itemView.setOnClickListener(if (enabled) null else onClickListener)
            viewBinding.deleteImageButton.isVisible = enabled
            itemView.setOnTouchListener(if (enabled) dragOnTouchListener else null)
        }

        fun onViewAttached() {
            isDraggingEnabled.observeForever(onDraggingEnabledChanged)
        }

        fun onViewDetached() {
            isDraggingEnabled.removeObserver(onDraggingEnabledChanged)
        }

        fun bind(image: Product.Image, viewType: Int) {
            if (viewType == VIEW_TYPE_PLACEHOLDER) {
                glideRequest.load(Uri.parse(image.source)).apply(glideTransform).into(viewBinding.productImage)
            } else if (viewType == VIEW_TYPE_IMAGE) {
                val photonUrl = PhotonUtils.getPhotonImageUrl(image.source, 0, imageSize)
                glideRequest.load(photonUrl).apply(glideTransform).into(viewBinding.productImage)
            }

            when (viewType) {
                VIEW_TYPE_PLACEHOLDER -> {
                    viewBinding.productImage.visibility = View.VISIBLE
                    viewBinding.productImage.alpha = 0.5F
                    viewBinding.uploadProgess.visibility = View.VISIBLE
                    viewBinding.addImageContainer.visibility = View.GONE
                }
                VIEW_TYPE_ADD_IMAGE -> {
                    viewBinding.productImage.visibility = View.GONE
                    viewBinding.uploadProgess.visibility = View.GONE
                    viewBinding.addImageContainer.visibility = View.VISIBLE
                }
                else -> {
                    viewBinding.productImage.visibility = View.VISIBLE
                    viewBinding.productImage.alpha = 1.0F
                    viewBinding.uploadProgess.visibility = View.GONE
                    viewBinding.addImageContainer.visibility = View.GONE
                }
            }

            viewBinding.deleteImageButton.setOnClickListener {
                listener.onGalleryImageDeleteIconClicked(image)
            }
        }

        private fun setMargins() {
            (viewBinding.productImage.layoutParams as FrameLayout.LayoutParams).apply {
                val margin = if (isGridView) {
                    val additionalMarginToFitDeleteIcon = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
                    additionalMarginToFitDeleteIcon
                } else {
                    0
                }

                setMargins(margin, margin, margin, margin)
            }
        }
    }
}
