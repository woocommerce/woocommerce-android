package com.woocommerce.android.ui.products.variations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideRequests
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.variations.VariationListAdapter.VariationViewHolder
import kotlinx.android.synthetic.main.variation_list_item.view.*
import org.wordpress.android.util.PhotonUtils

class VariationListAdapter(
    private val context: Context,
    private val glideRequest: GlideRequests,
    private val loadMoreListener: OnLoadMoreListener,
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
        val holder = VariationViewHolder(
                LayoutInflater.from(context).inflate(R.layout.variation_list_item, parent, false)
        )
        holder.imgVariationOption.clipToOutline = true
        return holder
    }

    override fun onBindViewHolder(holder: VariationViewHolder, position: Int) {
        val productVariation = variationList[position]

        holder.txtVariationOptionName.text = productVariation.optionName

        val variationPurchasable = if (!productVariation.isPurchasable) {
            context.getString(R.string.product_variation_disabled)
        } else {
            null
        }
        holder.txtVariationOptionPriceAndStock.text = StringBuilder()
                .appendWithIfNotEmpty(variationPurchasable)
                .appendWithIfNotEmpty(productVariation.priceWithCurrency, context.getString(R.string.product_bullet))

        holder.txtVariationOptionPriceAndStock.text = StringBuilder()
                .appendWithIfNotEmpty(variationPurchasable)
                .appendWithIfNotEmpty(productVariation.priceWithCurrency, context.getString(R.string.product_bullet))

        productVariation.image?.let {
            val imageUrl = PhotonUtils.getPhotonImageUrl(it.source, imageSize, imageSize)
            glideRequest.load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgVariationOption)
        } ?: holder.imgVariationOption.setImageResource(R.drawable.ic_product)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }

        holder.itemView.setOnClickListener {
            onItemClick(variationList[position])
        }
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

    class VariationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgVariationOption: ImageView = view.variationOptionImage
        val txtVariationOptionName: TextView = view.variationOptionName
        val txtVariationOptionPriceAndStock: TextView = view.variationOptionPriceAndStock
    }
}
