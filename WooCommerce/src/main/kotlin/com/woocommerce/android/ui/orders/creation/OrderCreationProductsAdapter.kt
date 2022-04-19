package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationProductItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationProductsAdapter.ProductViewHolder
import com.woocommerce.android.ui.products.ProductStockStatus
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderCreationProductsAdapter(
    private val onProductClicked: (Order.Item) -> Unit,
    private val currencyFormatter: (BigDecimal) -> String,
    private val onIncreaseQuantity: (Long) -> Unit,
    private val onDecreaseQuantity: (Long) -> Unit
) : ListAdapter<ProductUIModel, ProductViewHolder>(ProductUIModelDiffCallback) {
    init {
        setHasStableIds(true)
    }

    var isEachQuantityButtonEnabled = false
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

    override fun getItemId(position: Int): Long = getItem(position).item.uniqueId

    inner class ProductViewHolder(private val binding: OrderCreationProductItemBinding) : ViewHolder(binding.root) {
        private val context = binding.root.context
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
                }
            )
        }

        fun bind(productModel: ProductUIModel) {
            binding.productName.text = productModel.item.name
            binding.stepperView.isMinusButtonEnabled = isEachQuantityButtonEnabled
            binding.stepperView.isPlusButtonEnabled = isEachQuantityButtonEnabled
            binding.stepperView.apply {
                value = productModel.item.quantity.toInt()
                contentDescription = context.getString(R.string.count, value.toString())
            }

            binding.productAttributes.text = buildString {
                if (productModel.item.isVariation && productModel.item.attributesDescription.isNotEmpty()) {
                    append(productModel.item.attributesDescription)
                } else {
                    if (productModel.isStockManaged && productModel.stockStatus == ProductStockStatus.InStock) {
                        append(
                            context.getString(
                                R.string.order_creation_product_stock_quantity,
                                productModel.stockQuantity.formatToString()
                            )
                        )
                    } else {
                        append(context.getString(productModel.stockStatus.stringResource))
                    }
                }
                append(" • ")
                append(currencyFormatter(productModel.item.total).replace(" ", "\u00A0"))
            }

            binding.productSku.text = if (productModel.item.sku.isNotEmpty()) {
                context.getString(R.string.orderdetail_product_lineitem_sku_value, productModel.item.sku)
            } else {
                context.getString(R.string.no_sku)
            }

            val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_major_50)
            PhotonUtils.getPhotonImageUrl(productModel.imageUrl, imageSize, imageSize)?.let { imageUrl ->
                GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productIcon)
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
