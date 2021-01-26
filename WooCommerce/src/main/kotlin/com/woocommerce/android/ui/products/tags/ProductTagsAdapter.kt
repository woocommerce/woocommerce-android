package com.woocommerce.android.ui.products.tags

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductTagListItemBinding
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.tags.ProductTagsAdapter.ProductTagViewHolder

class ProductTagsAdapter(
    private val context: Context,
    private val loadMoreListener: OnLoadMoreListener,
    private val clickListener: OnProductTagClickListener
) : RecyclerView.Adapter<ProductTagViewHolder>() {
    private val productTags = ArrayList<ProductTag>()
    private var currentFilter: String = ""

    init {
        setHasStableIds(true)
    }

    interface OnProductTagClickListener {
        fun onProductTagAdded(productTag: ProductTag)
        fun onProductTagRemoved(productTag: ProductTag)
    }

    override fun getItemId(position: Int) = productTags[position].remoteTagId

    override fun getItemCount() = productTags.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductTagViewHolder {
        return ProductTagViewHolder(
            ProductTagListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductTagViewHolder, position: Int) {
        holder.bind(productTags[position])

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    fun setProductTags(productsTags: List<ProductTag>) {
        if (this.productTags.isEmpty()) {
            this.productTags.addAll(productsTags)
            notifyDataSetChanged()
        } else {
            val diffResult =
                DiffUtil.calculateDiff(ProductTagItemDiffUtil(this.productTags, productsTags))
            this.productTags.clear()
            this.productTags.addAll(productsTags)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    fun hasFilter() = currentFilter.isNotEmpty()

    /**
     * Sets the filter used to highlight matches in the tag list - note that the actual filtering is done
     * in the view model
     */
    fun setFilter(filter: String) {
        currentFilter = filter
        notifyDataSetChanged()
    }

    inner class ProductTagViewHolder(val viewBinding: ProductTagListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(productTag: ProductTag) {
            if (hasFilter()) {
                // if there's a filter, highlight it in the tag name - note that the tag name should always
                // match the filter, but we make sure the match is found (start > -1) as a precaution
                val start = productTag.name.indexOf(currentFilter, ignoreCase = true)
                if (start > -1) {
                    val sb = StringBuilder(productTag.name)
                    sb.insert(start, "<b>")
                    sb.insert(start + currentFilter.length + 3, "</b>")
                    viewBinding.tagItemName.text = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                } else {
                    viewBinding.tagItemName.text = productTag.name
                }
            } else {
                viewBinding.tagItemName.text = productTag.name
            }
            itemView.setOnClickListener { clickListener.onProductTagAdded(productTag) }
        }
    }

    private class ProductTagItemDiffUtil(
        val oldList: List<ProductTag>,
        val newList: List<ProductTag>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].remoteTagId == newList[newItemPosition].remoteTagId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }
}
