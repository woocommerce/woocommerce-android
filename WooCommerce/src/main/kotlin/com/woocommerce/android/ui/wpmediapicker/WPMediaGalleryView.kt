package com.woocommerce.android.ui.wpmediapicker

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WpmediaGalleryItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.di.GlideRequest
import com.woocommerce.android.model.Product
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCSavedState
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.PhotonUtils
import java.util.ArrayList
import java.util.Locale

/**
 * Custom recycler which displays images from the WP media library
 */
class WPMediaGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {
    companion object {
        const val NUM_COLUMNS = 3
        private const val SCALE_NORMAL = 1.0f
        private const val SCALE_SELECTED = .8f
        private const val KEY_RECYCLER_STATE = "recycler_state"
        private const val KEY_SELECTED_IMAGES = "selected_images"
        private const val KEY_MULTI_SELECT_ALLOWED = "multi_select_allowed"
    }

    interface WPMediaGalleryListener {
        fun onRequestLoadMore()
        fun onSelectionCountChanged()
        fun onImageLongClicked(image: Product.Image)
    }

    var isMultiSelectionAllowed: Boolean = true
    private var imageSize = 0
    private val selectedIds = ArrayList<Long>()

    private val adapter: WPMediaLibraryGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: WPMediaGalleryListener

    init {
        layoutManager = GridLayoutManager(context, NUM_COLUMNS)
        itemAnimator = DefaultItemAnimator()
        layoutInflater = LayoutInflater.from(context)

        setHasFixedSize(true)

        adapter = WPMediaLibraryGalleryAdapter().also {
            it.setHasStableIds(true)
            this.setAdapter(it)
        }

        // cancel pending Glide request when a view is recycled
        val glideRequests = GlideApp.with(this)
        setRecyclerListener { holder ->
            glideRequests.clear((holder as WPMediaViewHolder).viewBinding.imageView)
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

        // base the image size on the screen width divided by columns, taking margin into account
        val screenWidth = DisplayUtils.getDisplayPixelWidth(context)
        val margin = context.resources.getDimensionPixelSize(R.dimen.minor_25)
        imageSize = (screenWidth / NUM_COLUMNS) - (margin * NUM_COLUMNS)
    }

    fun showImages(images: List<Product.Image>, listener: WPMediaGalleryListener, isMultiSelectionAllowed: Boolean) {
        this.listener = listener
        this.isMultiSelectionAllowed = isMultiSelectionAllowed
        adapter.showImages(images)
    }

    fun getSelectedCount() = selectedIds.size

    fun getSelectedImages() = adapter.getSelectedImages()

    private fun setSelectedImages(images: ArrayList<Product.Image>) {
        adapter.setSelectedImages(images)
    }

    fun onImageLongClicked(position: Int) {
        listener.onImageLongClicked(adapter.getImage(position))
    }

    private inner class WPMediaLibraryGalleryAdapter : RecyclerView.Adapter<WPMediaViewHolder>() {
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

        override fun getItemId(position: Int) = imageList[position].id

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WPMediaViewHolder {
            return WPMediaViewHolder(
                WpmediaGalleryItemBinding.inflate(
                    layoutInflater, parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: WPMediaViewHolder, position: Int) {
            val image = getImage(position)
            val isSelected = isMultiSelectionAllowed && isItemSelected(image.id)
            holder.bind(image, isSelected)

            if (position == itemCount - 1) {
                listener.onRequestLoadMore()
            }
        }

        private fun isItemSelected(imageId: Long) = selectedIds.contains(imageId)

        fun toggleItemSelected(holder: WPMediaViewHolder, position: Int) {
            val isSelected = isItemSelectedByPosition(position)
            if (!isMultiSelectionAllowed && selectedIds.size > 0 && !isSelected) {
                selectedIds.clear()
            }
            setItemSelectedByPosition(holder, position, !isSelected)
        }

        private fun isItemSelectedByPosition(position: Int) = isItemSelected(imageList[position].id)

        private fun getImageById(imageId: Long): Product.Image? {
            for (image in imageList) {
                if (image.id == imageId) {
                    return image
                }
            }
            return null
        }

        fun getSelectedImages(): ArrayList<Product.Image> {
            val images = ArrayList<Product.Image>()
            for (imageId in selectedIds) {
                getImageById(imageId)?.let {
                    images.add(it)
                }
            }
            return images
        }

        fun setSelectedImages(images: ArrayList<Product.Image>) {
            selectedIds.clear()

            if (isMultiSelectionAllowed) {
                for (image in images) {
                    selectedIds.add(image.id)
                }
            } else if (images.isNotEmpty()) {
                selectedIds.add(images.first().id)
            }
            notifyDataSetChanged()
        }

        private fun setItemSelectedByPosition(
            holder: WPMediaViewHolder,
            position: Int,
            selected: Boolean
        ) {
            if (isItemSelectedByPosition(position) == selected) {
                return
            }

            val imageId = imageList[position].id
            if (selected) {
                selectedIds.add(imageId)
            } else {
                selectedIds.remove(imageId)
            }

            if (isMultiSelectionAllowed) {
                // show and animate the count or check mark
                if (selected) {
                    holder.viewBinding.textSelectionCount.text = String.format(
                        Locale.getDefault(),
                        "%d",
                        selectedIds.indexOf(imageId) + 1
                    )
                } else {
                    holder.viewBinding.textSelectionCount.text = null
                }
                WooAnimUtils.pop(holder.viewBinding.textSelectionCount)
                holder.viewBinding.textSelectionCount.isVisible = selected
            }

            // scale the thumbnail based on whether it's selected
            if (selected) {
                WooAnimUtils.scale(
                        holder.viewBinding.imageView,
                        SCALE_NORMAL,
                        SCALE_SELECTED,
                        Duration.SHORT
                )
            } else {
                WooAnimUtils.scale(
                        holder.viewBinding.imageView,
                        SCALE_SELECTED,
                        SCALE_NORMAL,
                        Duration.SHORT
                )
            }

            // redraw after the scale animation completes
            val delayMs: Long = Duration.SHORT.toMillis(context)
            Handler().postDelayed({ notifyDataSetChanged() }, delayMs)

            // let the fragment know the count has changed
            listener.onSelectionCountChanged()
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()

        // save the recycler's stat
        super.onSaveInstanceState()?.let { recyclerState ->
            bundle.putParcelable(KEY_RECYCLER_STATE, WCSavedState(super.onSaveInstanceState(), recyclerState))
        }

        // save the selected images
        bundle.putParcelableArrayList(KEY_SELECTED_IMAGES, getSelectedImages())
        bundle.putBoolean(KEY_MULTI_SELECT_ALLOWED, isMultiSelectionAllowed)

        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        // restore the recycler's state
        (state as? Bundle)?.getParcelable<WCSavedState>(KEY_RECYCLER_STATE)?.let { recyclerState ->
            super.onRestoreInstanceState(recyclerState)
        }

        // restore the selected images
        (state as? Bundle)?.getParcelableArrayList<Product.Image>(KEY_SELECTED_IMAGES)?.let { images ->
            setSelectedImages(images)
        }

        // restore multi-selection
        (state as? Bundle)?.getBoolean(KEY_MULTI_SELECT_ALLOWED)?.let { isAllowed ->
            isMultiSelectionAllowed = isAllowed
        }
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    inner class WPMediaViewHolder(val viewBinding: WpmediaGalleryItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.imageView.layoutParams.height = imageSize
            viewBinding.imageView.layoutParams.width = imageSize

            itemView.setOnClickListener {
                if (adapterPosition > NO_POSITION) {
                    adapter.toggleItemSelected(this, adapterPosition)
                }
            }

            itemView.setOnLongClickListener {
                if (adapterPosition > NO_POSITION) {
                    onImageLongClicked(adapterPosition)
                }
                true
            }
        }

        fun bind(image: Product.Image, isSelected: Boolean) {
            val photonUrl = PhotonUtils.getPhotonImageUrl(image.source, 0, imageSize)
            glideRequest.load(photonUrl).apply(glideTransform).into(viewBinding.imageView)

            viewBinding.textSelectionCount.isVisible = isMultiSelectionAllowed
            if (isMultiSelectionAllowed) {
                viewBinding.textSelectionCount.isSelected = isSelected
                if (isSelected) {
                    val count = selectedIds.indexOf(image.id) + 1
                    viewBinding.textSelectionCount.text = String.format(Locale.getDefault(), "%d", count)
                } else {
                    viewBinding.textSelectionCount.text = null
                }
            }

            // make sure the thumbnail scale reflects its selection state
            val scale: Float = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            if (viewBinding.imageView.scaleX != scale) {
                viewBinding.imageView.scaleX = scale
                viewBinding.imageView.scaleY = scale
            }
        }
    }
}
