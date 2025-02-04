package com.woocommerce.android.ui.products.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
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
) : ListAdapter<Product, ProductItemViewHolder>(ProductItemDiffCallback) {
    // allow the selection library to track the selections of the user
    var tracker: SelectionTracker<Long>? = null
    private var activeUploadIds = setOf<Long>()

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

    override fun getItemId(position: Int) = getItem(position).remoteId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductItemViewHolder {
        return ProductItemViewHolder(
            ProductListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = getItem(position)

        holder.bind(
            product,
            currencyFormatter,
            isActivated = tracker?.isSelected(product.remoteId) ?: false,
            isUploadingMedia = activeUploadIds.contains(product.remoteId),
            isProductHighlighted = isProductHighlighted(product.remoteId),
            isLastItem = position == itemCount - 1,
        )

        holder.itemView.setOnClickListener {
            clickListener?.invoke(product.remoteId, holder.itemView)
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }
}
