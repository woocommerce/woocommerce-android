package com.woocommerce.android.ui.whatsnew

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementListItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.FeatureAnnouncementItem
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementListAdapter.FeatureAnnouncementViewHolder
import com.woocommerce.android.util.WooLog

class FeatureAnnouncementListAdapter :
    ListAdapter<FeatureAnnouncementItem, FeatureAnnouncementViewHolder>(ItemDiffCallback) {
    init {
        setHasStableIds(true)
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

    override fun getItemId(position: Int) = getItem(position).title.hashCode().toLong()

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
                    loadUrlIntoCircle(
                        context = viewBinding.root.context,
                        imageView = viewBinding.featureItemIcon,
                        imgUrl = item.iconUrl,
                        placeholder = placeholder
                    )
                }
                item.iconBase64.isNotEmpty() -> {
                    loadBase64IntoCircle(
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

        /**
         * Loads an image from the "imgUrl" into the ImageView and applies circle transformation.
         */
        private fun loadUrlIntoCircle(
            context: Context,
            imageView: ImageView,
            imgUrl: String,
            placeholder: Drawable?
        ) {
            GlideApp.with(context)
                .load(imgUrl)
                .placeholder(placeholder)
                .error(placeholder)
                .circleCrop()
                .into(imageView)
                .clearOnDetach()
        }

        /**
         * Loads a base64 string without prefix (data:image/png;base64,) into the ImageView and applies circle
         * transformation.
         */
        private fun loadBase64IntoCircle(
            context: Context,
            imageView: ImageView,
            base64ImageData: String,
            placeholder: Drawable?
        ) {
            val imageData: ByteArray
            try {
                val sanitizedBase64String = base64ImageData.replace("data:image/png;base64,", "")
                imageData = Base64.decode(sanitizedBase64String, Base64.DEFAULT)
            } catch (ex: IllegalArgumentException) {
                WooLog.e(WooLog.T.MEDIA, "Cant parse base64 image data: ${ex.message}")
                return
            }

            GlideApp.with(context)
                .load(imageData)
                .placeholder(placeholder)
                .error(placeholder)
                .circleCrop()
                .into(imageView)
                .clearOnDetach()
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
