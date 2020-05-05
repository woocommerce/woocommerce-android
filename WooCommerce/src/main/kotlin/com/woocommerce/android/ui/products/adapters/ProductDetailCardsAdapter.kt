package com.woocommerce.android.ui.products.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.woocommerce.android.ui.products.models.ProductDetailCard
import com.woocommerce.android.ui.products.viewholders.ProductDetailCardViewHolder

class ProductDetailCardsAdapter : Adapter<ProductDetailCardViewHolder>() {
    private var items = listOf<ProductDetailCard>()

    fun update(newItems: List<ProductDetailCard>) {
        val diffResult = DiffUtil.calculateDiff(
            ProductDetailCardsDiffCallback(
                items,
                newItems
            )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailCardViewHolder {
        return ProductDetailCardViewHolder(parent)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ProductDetailCardViewHolder, position: Int) {
        holder.bind(items[position])
    }
}
