package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.di.GlideApp
import kotlinx.android.synthetic.main.product_list_item.view.*
import org.wordpress.android.fluxc.model.WCProductImageModel
import org.wordpress.android.util.PhotonUtils

class WCProductImageGalleryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {
    companion object {
        private var imageHeight = 0
    }

    private val adapter: ImageGalleryAdapter

    init {
        layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false)
        itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        adapter = ImageGalleryAdapter(context).also { setAdapter(it) }
        setHasFixedSize(false)
    }

    fun showImages(images: List<WCProductImageModel>) {
        imageHeight = this.height
        adapter.showImages(images)
    }

    private class ImageGalleryAdapter(private val context: Context) : RecyclerView.Adapter<ImageViewHolder>() {
        private val imageList = ArrayList<WCProductImageModel>()

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
            val imageUrl = imageList[position].src
            val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, 0, imageHeight)
            GlideApp.with(context)
                    .load(photonUrl)
                    .error(R.drawable.ic_product)
                    .placeholder(R.drawable.product_detail_image_background)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.productImage)
        }
    }

    private class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.productImage
    }
}
