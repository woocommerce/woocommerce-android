package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderCreationProductItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.creation.ProductsAdapter.ProductViewHolder
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class ProductsAdapter(
    private val productImageMap: ProductImageMap,
    private val currencyFormatter: (BigDecimal) -> String
) : RecyclerView.Adapter<ProductViewHolder>() {
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
        private val context = binding.root.context

        fun bind(productModel: ProductUIModel) {
            binding.productName.text = productModel.item.name
            binding.stepperView.value = productModel.item.quantity.toInt()

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
            PhotonUtils.getPhotonImageUrl(
                productImageMap.get(productModel.item.uniqueId), imageSize, imageSize
            )?.let { imageUrl ->
                GlideApp.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(binding.productIcon)
            }
        }
    }
}
