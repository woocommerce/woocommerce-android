package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationProductItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationProductsAdapter.ProductViewHolder
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderCreationProductsAdapter(
    private val onProductClicked: (Order.Item) -> Unit,
    private val currencyFormatter: (BigDecimal) -> String,
    private val onIncreaseQuantity: (Long) -> Unit,
    private val onDecreaseQuantity: (Long) -> Unit
) : RecyclerView.Adapter<ProductViewHolder>() {
    var products: List<ProductUIModel> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(ProductUIModelDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
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
        private val context = binding.root.context
        private val safePosition: Int?
            get() = adapterPosition.takeIf { it != NO_POSITION }

        init {
            binding.root.setOnClickListener {
                safePosition?.let {
                    onProductClicked(products[it].item)
                }
            }
            binding.stepperView.init(
                onPlusButtonClick = {
                    safePosition?.let { onIncreaseQuantity(products[it].item.uniqueId) }
                },
                onMinusButtonClick = {
                    safePosition?.let { onDecreaseQuantity(products[it].item.uniqueId) }
                }
            )
        }

        fun bind(productModel: ProductUIModel) {
            binding.productName.text = productModel.item.name
            binding.stepperView.apply {
                value = productModel.item.quantity.toInt()
                isMinusButtonEnabled = productModel.canDecreaseQuantity
            }

            binding.productAttributes.text = buildString {
                if (productModel.isStockManaged) {
                    append(
                        context.getString(
                            R.string.order_creation_product_stock_quantity,
                            productModel.stockQuantity.formatToString()
                        )
                    )
                } else {
                    append(context.getString(R.string.order_creation_product_instock))
                }
                append(" • ")
                append(currencyFormatter(productModel.item.total))
            }

            binding.productSku.text =
                context.getString(R.string.orderdetail_product_lineitem_sku_value, productModel.item.sku)

            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_major_50)
            PhotonUtils.getPhotonImageUrl(productModel.imageUrl, imageSize, imageSize)?.let { imageUrl ->
                GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productIcon)
            }
        }
    }

    private data class ProductUIModelDiffCallback(
        private val oldItems: List<ProductUIModel>,
        private val newItems: List<ProductUIModel>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].item.uniqueId == newItems[newItemPosition].item.uniqueId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}
