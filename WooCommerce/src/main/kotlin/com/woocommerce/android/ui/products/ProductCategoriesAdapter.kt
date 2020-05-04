package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CATEGORY_LIST_ITEM_TAPPED
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.ui.products.ProductCategoriesAdapter.ProductCategoryViewHolder
import kotlinx.android.synthetic.main.product_category_list_item.view.*
import org.wordpress.android.util.HtmlUtils

class ProductCategoriesAdapter(
    private val context: Context,
    private val clickListener: OnProductCategoryClickListener,
    private val loadMoreListener: OnLoadMoreListener
) : RecyclerView.Adapter<ProductCategoryViewHolder>() {
    private val productCategoryList = ArrayList<ProductCategoryViewHolderModel>()

    interface OnProductCategoryClickListener {
        fun onProductCategoryClick(productCategoryViewHolderModel: ProductCategoryViewHolderModel)
    }

    data class ProductCategoryViewHolderModel(
        val category: ProductCategory,
        var padding: Int = 0,
        var isSelected: Boolean = false
    )

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productCategoryList[position].category.remoteId

    override fun getItemCount() = productCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCategoryViewHolder {
        return ProductCategoryViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.product_category_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductCategoryViewHolder, position: Int) {
        val productCategory = productCategoryList[position]

        holder.txtCategoryName.text = if (productCategory.category.name.isEmpty()) {
            context.getString(R.string.untitled)
        } else {
            HtmlUtils.fastStripHtml(productCategory.category.name)
        }

        holder.txtCategoryName.setPaddingRelative(productCategory.padding, 0, 0, 0)

        holder.checkBox.isChecked = productCategory.isSelected

        holder.checkBox.setOnClickListener {
            handleCategoryClick(holder, productCategory)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
            handleCategoryClick(holder, productCategory)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private fun handleCategoryClick(
        holder: ProductCategoryViewHolder,
        productCategory: ProductCategoryViewHolderModel
    ) {
        productCategory.isSelected = holder.checkBox.isChecked
        AnalyticsTracker.track(CATEGORY_LIST_ITEM_TAPPED)
        clickListener.onProductCategoryClick(productCategory)
    }

    fun setProductCategories(productsCategories: List<ProductCategoryViewHolderModel>) {
        if (productCategoryList.isEmpty()) {
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            notifyDataSetChanged()
        } else {
            val diffResult =
                    DiffUtil.calculateDiff(ProductItemDiffUtil(productCategoryList, productsCategories))
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoryName: TextView = view.categoryName
        val checkBox: CheckBox = view.categorySelected
    }

    private class ProductItemDiffUtil(
        val items: List<ProductCategoryViewHolderModel>,
        val result: List<ProductCategoryViewHolderModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].category.remoteId == result[newItemPosition].category.remoteId

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.category.isSameCategory(newItem.category)
        }
    }
}
