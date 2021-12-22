package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.databinding.OrderCreationProductItemBinding
import com.woocommerce.android.ui.orders.creation.ProductsAdapter.ProductViewHolder

class ProductsAdapter : RecyclerView.Adapter<ProductViewHolder>() {
    var products: List<ProductUIModel> = emptyList()
        set(value) {
            field = value
            // TODO
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(
            OrderCreationProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(private val binding: OrderCreationProductItemBinding) : ViewHolder(binding.root) {
        fun bind(productModel: ProductUIModel) {
            // TODO
        }
    }
}

