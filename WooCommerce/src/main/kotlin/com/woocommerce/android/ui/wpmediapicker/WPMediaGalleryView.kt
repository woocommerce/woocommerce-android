package com.woocommerce.android.ui.wpmediapicker

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
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
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.BorderedImageView
import com.woocommerce.android.widgets.WCSavedState
import kotlinx.android.synthetic.main.wpmedia_gallery_item.view.*
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
    }

    interface WPMediaGalleryListener {
        fun onRequestLoadMore()
        fun onSelectionCountChanged()
        fun onImageLongClicked(image: Product.Image)
    }

    private var imageSize = 0
    private val selectedIds = ArrayList<Long>()

    private val adapter: WPMediaLibraryGalleryAdapter
    private val layoutInflater: LayoutInflater

    private val glideRequest: GlideRequest<Drawable>
    private val glideTransform: RequestOptions

    private lateinit var listener: WPMediaGalleryListener

    var allowMultiSelect: Boolean = true

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
            glideRequests.clear((holder as WPMediaViewHolder).imageView)
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

    fun showImages(images: List<Product.Image>, listener: WPMediaGalleryListener) {
        this.listener = listener
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
                layoutInflater.inflate(R.layout.wpmedia_gallery_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: WPMediaViewHolder, position: Int) {
            val image = getImage(position)
            val photonUrl = PhotonUtils.getPhotonImageUrl(image.source, 0, imageSize)
            glideRequest.load(photonUrl).apply(glideTransform).into(holder.imageView)

            val isSelected = isItemSelected(image.id)
            holder.textSelectionCount.isSelected = isSelected
            if (isSelected) {
                val count = selectedIds.indexOf(image.id) + 1
                holder.textSelectionCount.text = if (allowMultiSelect) String.format(Locale.getDefault(), "%d", count)
                else "✓"
            } else {
                holder.textSelectionCount.text = null
            }

            // make sure the thumbnail scale reflects its selection state
            val scale: Float = if (isSelected) SCALE_SELECTED else SCALE_NORMAL
            if (holder.imageView.scaleX != scale) {
                holder.imageView.scaleX = scale
                holder.imageView.scaleY = scale
            }

            if (position == itemCount - 1) {
                listener.onRequestLoadMore()
            }
        }

        private fun isItemSelected(imageId: Long) = selectedIds.contains(imageId)

        fun toggleItemSelected(holder: WPMediaViewHolder, position: Int) {
            val isSelected = isItemSelectedByPosition(position)
            if (!isSelected && !allowMultiSelect) {
                selectedIds.getOrNull(0)?.let { id ->
                    val previousSelectedPosition = imageList.indexOfFirst { it.id == id }
                    setItemSelectedByPosition(holder, previousSelectedPosition, false)
                }
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
            for (image in images) {
                selectedIds.add(image.id)
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

            // show and animate the count or check mark
            if (selected) {
                holder.textSelectionCount.text = if (allowMultiSelect) String.format(
                    Locale.getDefault(),
                    "%d",
                    selectedIds.indexOf(imageId) + 1
                ) else "✓"
            } else {
                holder.textSelectionCount.text = null
            }
            WooAnimUtils.pop(holder.textSelectionCount)
            holder.textSelectionCount.isVisible = selected

            // scale the thumbnail based on whether it's selected
            if (selected) {
                WooAnimUtils.scale(
                        holder.imageView,
                        SCALE_NORMAL,
                        SCALE_SELECTED,
                        Duration.SHORT
                )
            } else {
                WooAnimUtils.scale(
                        holder.imageView,
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
    }

    override fun dispatchSaveInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchFreezeSelfOnly(container)
    }

    override fun dispatchRestoreInstanceState(container: SparseArray<Parcelable>?) {
        super.dispatchThawSelfOnly(container)
    }

    private inner class WPMediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: BorderedImageView = view.imageView
        val textSelectionCount: TextView = view.textSelectionCount

        init {
            imageView.layoutParams.height = imageSize
            imageView.layoutParams.width = imageSize

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
    }
}
