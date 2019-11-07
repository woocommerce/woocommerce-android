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
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.ui.products.ProductVariantsAdapter.ProductVariantViewHolder
import kotlinx.android.synthetic.main.product_variant_list_item.view.*
import org.wordpress.android.util.PhotonUtils

class ProductVariantsAdapter(
    private val context: Context
) : RecyclerView.Adapter<ProductVariantViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
    private val productVariantList = ArrayList<ProductVariant>()

    override fun getItemId(position: Int) = productVariantList[position].remoteProductId

    override fun getItemCount() = productVariantList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductVariantViewHolder {
        val holder = ProductVariantViewHolder(LayoutInflater.from(context).inflate(R.layout.product_variant_list_item, parent, false))
        holder.imgVariantOption.clipToOutline = true
        return holder
    }

    override fun onBindViewHolder(holder: ProductVariantViewHolder, position: Int) {
        val productVariant = productVariantList[position]

        holder.txtVariantOptionName.text = productVariant.optionName
        productVariant.priceWithCurrency?.let {
            holder.txtVariantOptionPriceAndStock.text = it
        }

        productVariant.imageUrl?.let {
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.imgVariantOption)
        } ?: holder.imgVariantOption.setImageResource(R.drawable.ic_product)
    }

    private class ProductVariantItemDiffUtil(
        val items: List<ProductVariant>,
        val result: List<ProductVariant>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].remoteProductId == result[newItemPosition].remoteProductId

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.stockQuantity == newItem.stockQuantity &&
                    oldItem.stockStatus == newItem.stockStatus &&
                    oldItem.imageUrl == newItem.imageUrl &&
                    oldItem.optionName == newItem.optionName
        }
    }

    fun setProductVariantList(productVariants: List<ProductVariant>) {
        val diffResult = DiffUtil.calculateDiff(
                ProductVariantItemDiffUtil(productVariantList, productVariants)
        )
        productVariantList.clear()
        productVariantList.addAll(productVariants)
        diffResult.dispatchUpdatesTo(this)
    }

    class ProductVariantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgVariantOption: ImageView = view.variantOptionImage
        val txtVariantOptionName: TextView = view.variantOptionName
        val txtVariantOptionPriceAndStock: TextView = view.variantOptionPriceAndStock
    }
}
