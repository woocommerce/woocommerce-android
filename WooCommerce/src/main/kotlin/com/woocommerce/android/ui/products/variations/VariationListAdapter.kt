package com.woocommerce.android.ui.products.variations

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.R
import com.woocommerce.android.databinding.VariationListItemBinding
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.variations.VariationListAdapter.VariationViewHolder
import org.wordpress.android.util.PhotonUtils

class VariationListAdapter(
    private val context: Context,
    private val glideRequest: RequestManager,
    private val loadMoreListener: OnLoadMoreListener,
    private val parentProduct: Product?,
    private val onItemClick: (variation: ProductVariation) -> Unit
) : ListAdapter<ProductVariation, VariationViewHolder>(VariationItemDiffCallback) {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).remoteVariationId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariationViewHolder {
        val holder = VariationViewHolder(
            VariationListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        holder.itemView.setOnClickListener {
            onItemClick(getItem(holder.bindingAdapterPosition))
        }
        return holder
    }

    override fun onBindViewHolder(holder: VariationViewHolder, position: Int) {
        val variation = getItem(position)
        holder.bind(variation)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private fun ProductVariation.getStockStatusText(): String {
        return when (stockStatus) {
            InStock -> {
                context.getString(R.string.product_stock_status_instock)
            }
            OutOfStock -> {
                context.getString(R.string.product_stock_status_out_of_stock)
            }
            OnBackorder -> {
                context.getString(R.string.product_stock_status_on_backorder)
            }
            else -> {
                stockStatus.value
            }
        }
    }

    private fun highlightText(text: String): SpannableString {
        val spannable = SpannableString(text)
        spannable.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.warning_banner_foreground_color)),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    inner class VariationViewHolder(val viewBinding: VariationListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        private val imageCornerRadius = itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
        fun bind(variation: ProductVariation) {
            viewBinding.variationOptionName.text = variation.getName(parentProduct)

            val stockStatus = variation.getStockStatusText()
            val bullet = context.getString(R.string.product_bullet)
            viewBinding.variationOptionPriceAndStock.text = if (variation.isVisible) {
                if (variation.isSaleInEffect || variation.regularPrice.isSet()) {
                    StringBuilder(stockStatus).appendWithIfNotEmpty(variation.priceWithCurrency, bullet)
                } else {
                    val highlightedText = highlightText(context.getString(R.string.product_variation_no_price_set))
                    TextUtils.concat(stockStatus, bullet, highlightedText)
                }
            } else {
                StringBuilder(stockStatus)
                    .appendWithIfNotEmpty(context.getString(R.string.product_variation_disabled), bullet)
                    .appendWithIfNotEmpty(variation.priceWithCurrency, bullet)
            }

            viewBinding.variationOptionImage.clipToOutline = true

            variation.image?.let {
                val imageUrl = PhotonUtils.getPhotonImageUrl(it.source, imageSize, imageSize)
                glideRequest.load(imageUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                    .placeholder(R.drawable.ic_product)
                    .into(viewBinding.variationOptionImage)
            } ?: viewBinding.variationOptionImage.setImageResource(R.drawable.ic_product)
        }
    }

    object VariationItemDiffCallback : DiffUtil.ItemCallback<ProductVariation>() {
        override fun areItemsTheSame(
            oldItem: ProductVariation,
            newItem: ProductVariation
        ): Boolean {
            return oldItem.remoteVariationId == newItem.remoteVariationId
        }

        override fun areContentsTheSame(
            oldItem: ProductVariation,
            newItem: ProductVariation
        ): Boolean {
            return oldItem == newItem
        }
    }
}
