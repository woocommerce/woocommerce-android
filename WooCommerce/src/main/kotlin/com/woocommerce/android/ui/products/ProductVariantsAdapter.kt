package com.woocommerce.android.ui.products

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
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.products.ProductVariantsAdapter.ProductVariantViewHolder
import kotlinx.android.synthetic.main.product_variant_list_item.view.*
import org.wordpress.android.util.PhotonUtils

class ProductVariantsAdapter(
    private val context: Context,
    private val glideRequest: GlideRequests,
    private val loadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<ProductVariantViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
    private val productVariantList = ArrayList<ProductVariant>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productVariantList[position].remoteVariationId

    override fun getItemCount() = productVariantList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductVariantViewHolder {
        val holder = ProductVariantViewHolder(
                LayoutInflater.from(context).inflate(R.layout.product_variant_list_item, parent, false)
        )
        holder.imgVariantOption.clipToOutline = true
        return holder
    }

    override fun onBindViewHolder(holder: ProductVariantViewHolder, position: Int) {
        val productVariant = productVariantList[position]

        holder.txtVariantOptionName.text = productVariant.optionName

        val variantPurchasable = if (!productVariant.purchasable) {
            context.getString(R.string.product_variant_hidden)
        } else {
            null
        }
        holder.txtVariantOptionPriceAndStock.text = StringBuilder()
                .appendWithIfNotEmpty(variantPurchasable)
                .appendWithIfNotEmpty(productVariant.priceWithCurrency, context.getString(R.string.product_bullet))

        holder.txtVariantOptionPriceAndStock.text = StringBuilder()
                .appendWithIfNotEmpty(variantPurchasable)
                .appendWithIfNotEmpty(productVariant.priceWithCurrency, context.getString(R.string.product_bullet))

        productVariant.imageUrl?.let {
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            glideRequest.load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgVariantOption)
        } ?: holder.imgVariantOption.setImageResource(R.drawable.ic_product)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private class ProductVariantItemDiffUtil(
        val oldList: List<ProductVariant>,
        val newList: List<ProductVariant>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].remoteVariationId == newList[newItemPosition].remoteVariationId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.stockQuantity == newItem.stockQuantity &&
                    oldItem.remoteVariationId == newItem.remoteVariationId &&
                    oldItem.price == newItem.price &&
                    oldItem.purchasable == newItem.purchasable &&
                    oldItem.stockStatus == newItem.stockStatus &&
                    oldItem.imageUrl == newItem.imageUrl &&
                    oldItem.optionName == newItem.optionName
        }
    }

    fun setProductVariantList(productVariants: List<ProductVariant>) {
        fun isSameList(): Boolean {
            if (productVariants.size != productVariantList.size) {
                return false
            }
            for (index in productVariants.indices) {
                val oldItem = productVariantList[index]
                val newItem = productVariants[index]
                if (!oldItem.isSameVariant(newItem)) {
                    return false
                }
            }
            return true
        }

        if (!isSameList()) {
            val diffResult = DiffUtil.calculateDiff(
                    ProductVariantItemDiffUtil(productVariantList, productVariants)
            )
            productVariantList.clear()
            productVariantList.addAll(productVariants)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductVariantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgVariantOption: ImageView = view.variantOptionImage
        val txtVariantOptionName: TextView = view.variantOptionName
        val txtVariantOptionPriceAndStock: TextView = view.variantOptionPriceAndStock
    }
}
