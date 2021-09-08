package com.woocommerce.android.ui.whatsnew

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementListItemBinding
import com.woocommerce.android.model.FeatureAnnouncementItem
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementListAdapter.FeatureAnnouncementViewHolder
import com.woocommerce.android.util.ImageUtils

class FeatureAnnouncementListAdapter :
    ListAdapter<FeatureAnnouncementItem, FeatureAnnouncementViewHolder>(ItemDiffCallback) {
    fun updateData(uiModels: List<FeatureAnnouncementItem>) {
        submitList(uiModels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureAnnouncementViewHolder {
        return FeatureAnnouncementViewHolder(
            FeatureAnnouncementListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: FeatureAnnouncementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int) = position.toLong()

    class FeatureAnnouncementViewHolder(val viewBinding: FeatureAnnouncementListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: FeatureAnnouncementItem) {
            viewBinding.featureTitle.text = item.title
            viewBinding.featureSubtitle.text = item.subtitle

            val placeholder = ContextCompat.getDrawable(
                viewBinding.root.context,
                R.drawable.ic_info_outline_24dp
            )

            when {
                item.iconUrl.isNotEmpty() -> {
                    ImageUtils.loadUrlIntoCircle(
                        context = viewBinding.root.context,
                        imageView = viewBinding.featureItemIcon,
                        imgUrl = item.iconUrl,
                        placeholder = placeholder
                    )
                }
                item.iconBase64.isNotEmpty() -> {
                    ImageUtils.loadBase64IntoCircle(
                        context = viewBinding.root.context,
                        imageView = viewBinding.featureItemIcon,
                        base64ImageData = item.iconBase64,
                        placeholder = placeholder
                    )
                }
                else -> {
                    viewBinding.featureItemIcon.setImageDrawable(placeholder)
                }
            }
        }
    }

    object ItemDiffCallback : DiffUtil.ItemCallback<FeatureAnnouncementItem>() {
        override fun areItemsTheSame(
            oldItem: FeatureAnnouncementItem,
            newItem: FeatureAnnouncementItem
        ): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(
            oldItem: FeatureAnnouncementItem,
            newItem: FeatureAnnouncementItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
