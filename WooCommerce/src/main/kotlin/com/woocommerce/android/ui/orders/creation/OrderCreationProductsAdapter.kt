package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationProductItemBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationProductsAdapter.ProductViewHolder
import com.woocommerce.android.util.CurrencyFormatter

class OrderCreationProductsAdapter(
    private val onProductClicked: (Order.Item) -> Unit,
    private val currencyFormatter: CurrencyFormatter,
    private val currencyCode: String?,
    private val onIncreaseQuantity: (Long) -> Unit,
    private val onDecreaseQuantity: (Long) -> Unit
) : ListAdapter<ProductUIModel, ProductViewHolder>(ProductUIModelDiffCallback) {
    var areProductsEditable = false
        set(value) {
            if (value != field) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        return ProductViewHolder(
            OrderCreationProductItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: OrderCreationProductItemBinding) : ViewHolder(binding.root) {
        private val safePosition: Int?
            get() = bindingAdapterPosition.takeIf { it != NO_POSITION }

        init {
            binding.root.setOnClickListener {
                safePosition?.let {
                    onProductClicked(getItem(it).item)
                }
            }
            binding.stepperView.init(
                onPlusButtonClick = {
                    safePosition?.let { onIncreaseQuantity(getItem(it).item.itemId) }
                },
                onMinusButtonClick = {
                    safePosition?.let { onDecreaseQuantity(getItem(it).item.itemId) }
                },
                plusMinusContentDescription = R.string.order_creation_change_product_quantity
            )
            binding.productItemView.binding.divider.visibility = View.GONE
        }

        fun bind(productModel: ProductUIModel) {
            binding.root.isEnabled = productModel.item.isSynced() && areProductsEditable
            binding.productItemView.bind(productModel, currencyFormatter, currencyCode)

            binding.stepperView.isMinusButtonEnabled = areProductsEditable
            binding.stepperView.isPlusButtonEnabled = areProductsEditable
            binding.stepperView.apply {
                value = productModel.item.quantity.toInt()
                contentDescription = context.getString(R.string.count, value.toString())
            }
        }
    }

    object ProductUIModelDiffCallback : DiffUtil.ItemCallback<ProductUIModel>() {
        override fun areItemsTheSame(
            oldItem: ProductUIModel,
            newItem: ProductUIModel
        ): Boolean = oldItem.item.uniqueId == newItem.item.uniqueId

        override fun areContentsTheSame(
            oldItem: ProductUIModel,
            newItem: ProductUIModel
        ): Boolean = oldItem == newItem
    }
}
