package com.woocommerce.android.ui.products.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ParentCategoryListItemBinding
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.ParentCategoryListAdapter.ParentCategoryListViewHolder

class ParentCategoryListAdapter(
    private val context: Context,
    private var selectedCategoryId: Long,
    private val loadMoreListener: OnLoadMoreListener,
    private val clickListener: OnProductCategoryClickListener
) : RecyclerView.Adapter<ParentCategoryListViewHolder>() {
    var parentCategoryList: List<ProductCategoryItemUiModel> = ArrayList()
        set(value) {
            if (!isSameList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = parentCategoryList[position].category.remoteCategoryId

    override fun getItemCount() = parentCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentCategoryListViewHolder {
        return ParentCategoryListViewHolder(
            ParentCategoryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
        )
    }

    override fun onBindViewHolder(holder: ParentCategoryListViewHolder, position: Int) {
        val parentCategory = parentCategoryList[position]
        holder.bind(parentCategory)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private fun getParentCategoryAtPosition(position: Int) = parentCategoryList[position]

    private fun isSameList(categories: List<ProductCategoryItemUiModel>): Boolean {
        if (categories.size != parentCategoryList.size) {
            return false
        }

        categories.forEach {
            if (!containsParentCategory(it)) {
                return false
            }
        }

        return true
    }

    private fun containsParentCategory(parentCategory: ProductCategoryItemUiModel): Boolean {
        parentCategoryList.forEach {
            if (it.category.remoteCategoryId == parentCategory.category.remoteCategoryId) {
                return true
            }
        }
        return false
    }

    inner class ParentCategoryListViewHolder(private val viewBinder: ParentCategoryListItemBinding) :
        RecyclerView.ViewHolder(viewBinder.root) {
        init {
            viewBinder.root.setOnClickListener {
                val position = adapterPosition
                if (position > -1) {
                    getParentCategoryAtPosition(position).let {
                        selectedCategoryId = it.category.remoteCategoryId
                        clickListener.onProductCategoryClick(it)
                    }
                }
            }
        }

        fun bind(parentCategory: ProductCategoryItemUiModel) {
            viewBinder.parentCategoryName.apply {
                setHtmlText(parentCategory.category.name)

                val newLayoutParams = viewBinder.parentCategoryName.layoutParams as LayoutParams
                newLayoutParams.marginStart = parentCategory.margin
                layoutParams = newLayoutParams

                isChecked = selectedCategoryId == parentCategory.category.remoteCategoryId
            }
        }
    }
}
