package com.woocommerce.android.ui.products.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ComponentOptionItemViewBinding
import com.woocommerce.android.ui.products.ComponentOption
import org.wordpress.android.util.PhotonUtils

class ComponentOptionsListAdapter :
    ListAdapter<ComponentOption, ComponentOptionViewHolder>(ComponentOptionItemDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentOptionViewHolder {
        return ComponentOptionViewHolder(
            ComponentOptionItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ComponentOptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ComponentOptionViewHolder(val viewBinding: ComponentOptionItemViewBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {
    private val imageSize = itemView.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val imageCornerRadius = itemView.resources.getDimensionPixelSize(R.dimen.corner_radius_image)

    fun bind(option: ComponentOption) {
        viewBinding.componentOptionTitle.text = option.title
        if (option.shouldDisplayImage) {
            showProductImage(option.imageUrl)
        } else {
            viewBinding.componentOptionImageFrame.isVisible = false
        }
    }

    private fun showProductImage(imageUrl: String?) {
        val size: Int
        when {
            imageUrl.isNullOrEmpty() -> {
                size = imageSize / 2
                viewBinding.componentOptionImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                Glide.with(viewBinding.componentOptionImage).load(photonUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius)).placeholder(R.drawable.ic_product)
                    .into(viewBinding.componentOptionImage)
            }
        }
        viewBinding.componentOptionImage.layoutParams.apply {
            height = size
            width = size
        }
    }
}

object ComponentOptionItemDiffCallback : DiffUtil.ItemCallback<ComponentOption>() {
    override fun areItemsTheSame(
        oldItem: ComponentOption,
        newItem: ComponentOption
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ComponentOption,
        newItem: ComponentOption
    ): Boolean {
        return oldItem == newItem
    }
}
