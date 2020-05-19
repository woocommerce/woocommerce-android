package com.woocommerce.android.ui.products.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.viewholders.ProductPropertyCardViewHolder

class ProductPropertyCardsAdapter : Adapter<ProductPropertyCardViewHolder>() {
    private var items = listOf<ProductPropertyCard>()

    fun update(newItems: List<ProductPropertyCard>) {
        val diffResult = DiffUtil.calculateDiff(
            ProductPropertiesCardsDiffCallback(
                items,
                newItems
            )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductPropertyCardViewHolder {
        return ProductPropertyCardViewHolder(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductPropertyCardViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
