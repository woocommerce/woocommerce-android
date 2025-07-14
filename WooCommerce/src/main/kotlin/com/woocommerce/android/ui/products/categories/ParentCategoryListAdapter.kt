package com.woocommerce.android.ui.products.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ParentCategoryListItemBinding
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.ParentCategoryListAdapter.ParentCategoryListViewHolder

class ParentCategoryListAdapter(
    private var selectedCategoryId: Long,
    private val loadMoreListener: OnLoadMoreListener,
    private val clickListener: OnProductCategoryClickListener
) : ListAdapter<ProductCategoryItemUiModel, ParentCategoryListViewHolder>(ParentCategoryDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).category.remoteCategoryId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentCategoryListViewHolder {
        return ParentCategoryListViewHolder(
            ParentCategoryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ParentCategoryListViewHolder, position: Int) {
        val parentCategory = getItem(position)
        holder.bind(parentCategory)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    inner class ParentCategoryListViewHolder(private val viewBinder: ParentCategoryListItemBinding) :
        RecyclerView.ViewHolder(viewBinder.root) {
        init {
            viewBinder.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position > -1) {
                    getItem(position).let {
                        selectedCategoryId = it.category.remoteCategoryId
                        clickListener.onProductCategoryChecked(it)
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

    object ParentCategoryDiffCallback : DiffUtil.ItemCallback<ProductCategoryItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ProductCategoryItemUiModel,
            newItem: ProductCategoryItemUiModel
        ): Boolean {
            return oldItem.category.remoteCategoryId == newItem.category.remoteCategoryId
        }

        override fun areContentsTheSame(
            oldItem: ProductCategoryItemUiModel,
            newItem: ProductCategoryItemUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
