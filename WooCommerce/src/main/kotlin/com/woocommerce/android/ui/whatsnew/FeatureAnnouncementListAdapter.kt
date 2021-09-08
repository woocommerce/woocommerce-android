package com.woocommerce.android.ui.whatsnew

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementListItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.FeatureAnnouncementItem
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementListAdapter.FeatureAnnouncementViewHolder

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

            item.iconUrl.let {
                val placeholder = ContextCompat.getDrawable(
                    viewBinding.root.context,
                    R.drawable.ic_gridicons_credit_card
                )

                GlideApp.with(viewBinding.root.context)
                    .load(it)
                    .placeholder(placeholder)
                    .circleCrop()
                    .into(viewBinding.featureItemIcon)
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
