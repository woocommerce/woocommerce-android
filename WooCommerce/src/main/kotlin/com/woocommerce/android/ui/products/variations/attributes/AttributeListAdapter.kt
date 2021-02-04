package com.woocommerce.android.ui.products.variations.attributes

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductStockStatus.OnBackorder
import com.woocommerce.android.ui.products.ProductStockStatus.OutOfStock
import com.woocommerce.android.ui.products.variations.attributes.AttributeListAdapter.AttributeViewHolder

class AttributeListAdapter(
    private val context: Context,
    private val onItemClick: (attribute: ProductGlobalAttribute) -> Unit
) : RecyclerView.Adapter<AttributeViewHolder>() {
    private var attributeList = listOf<ProductGlobalAttribute>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = attributeList[position].remoteId.toLong()

    override fun getItemCount() = attributeList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributeViewHolder {
        return AttributeViewHolder(
            AttributeListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AttributeViewHolder, position: Int) {
        holder.bind(attributeList[position])

        holder.itemView.setOnClickListener {
            onItemClick(attributeList[position])
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
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.warning_foreground_color)),
            0,
            spannable.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private class AttributeItemDiffUtil(
        val oldList: List<ProductGlobalAttribute>,
        val newList: List<ProductGlobalAttribute>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].remoteId == newList[newItemPosition].remoteId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun setAttributeList(attributes: List<ProductGlobalAttribute>) {
        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                attributeList,
                attributes
            )
        )
        attributeList = attributes
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: ProductGlobalAttribute) {
            viewBinding.attributeName.text = attribute.name
            // TODO viewBinding.attributeTerms.text = ??
        }
    }
}
