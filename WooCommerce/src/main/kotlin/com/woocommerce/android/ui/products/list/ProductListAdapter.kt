package com.woocommerce.android.ui.products.list

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.ProductItemDiffCallback
import com.woocommerce.android.ui.products.ProductItemViewHolder
import com.woocommerce.android.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

typealias OnProductClickListener = (remoteProductId: Long, sharedView: View?) -> Unit

class ProductListAdapter(
    private val loadMoreListener: OnLoadMoreListener,
    private val currencyFormatter: CurrencyFormatter,
    private val isProductHighlighted: (Long) -> Boolean,
    private val clickListener: OnProductClickListener? = null,
    private val mediaFileUploadHandler: MediaFileUploadHandler? = null,
    coroutineScope: CoroutineScope? = null,
) : ListAdapter<Product, RecyclerView.ViewHolder>(ProductItemDiffCallback) {
    companion object {
        private const val VIEW_TYPE_PRODUCT_ITEM = 0
        private const val VIEW_TYPE_LOADING_MORE = 1
    }

    var tracker: SelectionTracker<Long>? = null
    private var activeUploadIds = setOf<Long>()
    private var isLoadingMore = false
    private val handler = Handler(Looper.getMainLooper())

    init {
        setHasStableIds(true)
        if (coroutineScope != null && mediaFileUploadHandler != null) {
            coroutineScope.launch {
                mediaFileUploadHandler.activeUploadProductIds.collect { newIds ->
                    val oldIds = activeUploadIds
                    activeUploadIds = newIds

                    currentList.forEachIndexed { index, product ->
                        if (newIds.contains(product.remoteId) != oldIds.contains(product.remoteId)) {
                            notifyItemChanged(index)
                        }
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        if (isLoadingMore && position == super.getItemCount()) {
            return -1L
        }
        return getItem(position)?.remoteId ?: -1L
    }

    override fun getItemViewType(position: Int): Int {
        if (isLoadingMore && position == super.getItemCount()) {
            return VIEW_TYPE_LOADING_MORE
        }
        return VIEW_TYPE_PRODUCT_ITEM
    }

    override fun getItemCount(): Int {
        val baseCount = super.getItemCount()
        return if (isLoadingMore) baseCount + 1 else baseCount
    }

    override fun getItem(position: Int): Product? {
        if (isLoadingMore && position == super.getItemCount()) {
            return null
        }
        return super.getItem(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PRODUCT_ITEM -> {
                ProductItemViewHolder(
                    ProductListItemBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_LOADING_MORE -> {
                val view = inflater.inflate(R.layout.list_loading_more_item, parent, false)
                LoadingViewHolder(view)
            }
            else -> error("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ProductItemViewHolder -> {
                val product = getItem(position)!!

                holder.bind(
                    product,
                    currencyFormatter,
                    isActivated = tracker?.isSelected(product.remoteId) ?: false,
                    isUploadingMedia = activeUploadIds.contains(product.remoteId),
                    isProductHighlighted = isProductHighlighted(product.remoteId),
                    isLastItem = position == super.getItemCount() - 1,
                )

                holder.itemView.setOnClickListener {
                    clickListener?.invoke(product.remoteId, holder.itemView)
                }

                if (position == super.getItemCount() - 1 && !isLoadingMore) {
                    loadMoreListener.onRequestLoadMore()
                }
            }
            is LoadingViewHolder -> {
                // No binding needed for loading view
            }
        }
    }

    override fun submitList(list: List<Product>?) {
        val oldItemCount = super.getItemCount()
        val hadLoadingIndicator = isLoadingMore

        super.submitList(list) {
            if (hadLoadingIndicator && !isLoadingMore && super.getItemCount() > oldItemCount) {
                notifyItemRemoved(oldItemCount)
            }
        }
    }

    fun setLoadingMoreIndicator(active: Boolean) {
        if (isLoadingMore != active) {
            isLoadingMore = active

            if (active) {
                val insertPosition = super.getItemCount()
                handler.post {
                    notifyItemInserted(insertPosition)
                }
            } else {
                val removePosition = super.getItemCount()
                handler.post {
                    notifyItemRemoved(removePosition)
                }
            }
        }
    }

    private class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
