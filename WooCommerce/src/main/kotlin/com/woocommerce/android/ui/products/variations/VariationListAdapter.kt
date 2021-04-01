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
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.VariationListItemBinding
import com.woocommerce.android.di.GlideRequests
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
    private val glideRequest: GlideRequests,
    private val loadMoreListener: OnLoadMoreListener,
    private val parentProduct: Product?,
    private val onItemClick: (variation: ProductVariation) -> Unit
) : RecyclerView.Adapter<VariationViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private var variationList = listOf<ProductVariation>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = variationList[position].remoteVariationId

    override fun getItemCount() = variationList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VariationViewHolder {
        return VariationViewHolder(
            VariationListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VariationViewHolder, position: Int) {
        val variation = variationList[position]
        holder.bind(variation)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }

        holder.itemView.setOnClickListener {
            onItemClick(variation)
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

    private class VariationItemDiffUtil(
        val oldList: List<ProductVariation>,
        val newList: List<ProductVariation>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].remoteVariationId == newList[newItemPosition].remoteVariationId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun setVariationList(variations: List<ProductVariation>) {
        val diffResult = DiffUtil.calculateDiff(
            VariationItemDiffUtil(
                variationList,
                variations
            )
        )
        variationList = variations
        diffResult.dispatchUpdatesTo(this)
    }

    inner class VariationViewHolder(val viewBinding: VariationListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
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
                    .placeholder(R.drawable.ic_product)
                    .into(viewBinding.variationOptionImage)
            } ?: viewBinding.variationOptionImage.setImageResource(R.drawable.ic_product)
        }
    }
}
