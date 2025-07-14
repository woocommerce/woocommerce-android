package com.woocommerce.android.ui.products.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ComponentItemViewBinding
import com.woocommerce.android.model.Component
import org.wordpress.android.util.PhotonUtils

class ComponentsListAdapter(private val clickListener: OnComponentClickListener) :
    ListAdapter<Component, ComponentViewHolder>(ComponentItemDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).id

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ComponentViewHolder {
        return ComponentViewHolder(
            ComponentItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ComponentViewHolder, position: Int) {
        val component = getItem(position)
        holder.bind(component)
        holder.itemView.setOnClickListener {
            clickListener.onComponentClickListener(component)
        }
    }

    interface OnComponentClickListener {
        fun onComponentClickListener(component: Component)
    }
}

class ComponentViewHolder(val viewBinding: ComponentItemViewBinding) : RecyclerView.ViewHolder(viewBinding.root) {
    private val imageSize = itemView.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private val imageCornerRadius = itemView.resources.getDimensionPixelSize(R.dimen.corner_radius_image)

    fun bind(component: Component) {
        viewBinding.componentName.text = component.title
        showDescriptionHTML(component.description)
        showProductImage(component.thumbnailUrl)
    }

    private fun showDescriptionHTML(description: String) {
        if (description.isNotEmpty()) {
            viewBinding.componentDescription.isVisible = true
            viewBinding.componentDescription.text = HtmlCompat.fromHtml(
                description,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
        } else {
            viewBinding.componentDescription.isVisible = false
        }
    }

    private fun showProductImage(imageUrl: String?) {
        val size: Int
        when {
            imageUrl.isNullOrEmpty() -> {
                size = imageSize / 2
                viewBinding.componentImage.setImageResource(R.drawable.ic_product)
            }
            else -> {
                size = imageSize
                val photonUrl = PhotonUtils.getPhotonImageUrl(imageUrl, imageSize, imageSize)
                Glide.with(viewBinding.componentImage).load(photonUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius)).placeholder(R.drawable.ic_product)
                    .into(viewBinding.componentImage)
            }
        }
        viewBinding.componentImage.layoutParams.apply {
            height = size
            width = size
        }
    }
}

object ComponentItemDiffCallback : DiffUtil.ItemCallback<Component>() {
    override fun areItemsTheSame(
        oldItem: Component,
        newItem: Component
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: Component,
        newItem: Component
    ): Boolean {
        return oldItem == newItem
    }
}
