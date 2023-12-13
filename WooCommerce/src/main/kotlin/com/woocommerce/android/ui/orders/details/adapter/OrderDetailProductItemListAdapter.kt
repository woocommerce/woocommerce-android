package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailProductGroupItemBinding
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.orders.OrderDetailProductItemView
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.ViewAddonClickListener
import com.woocommerce.android.ui.orders.details.OrderProduct
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class OrderDetailProductItemListAdapter(
    private val productItems: List<OrderProduct>,
    private val productImageMap: ProductImageMap,
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val productItemListener: OrderProductActionListener,
    private val onViewAddonsClick: ViewAddonClickListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private inner class ProductItemViewHolder(val view: OrderDetailProductItemView) : RecyclerView.ViewHolder(view) {
        fun onBind(
            productItem: OrderProduct.ProductItem,
            productImageMap: ProductImageMap,
            formatCurrencyForDisplay: (BigDecimal) -> String,
            productItemListener: OrderProductActionListener,
            onViewAddonsClick: ViewAddonClickListener? = null
        ) {
            val item = productItem.product
            val imageSize = view.resources.getDimensionPixelSize(R.dimen.image_major_50)
            val productImage = PhotonUtils.getPhotonImageUrl(productImageMap.get(item.uniqueId), imageSize, imageSize)
            view.initView(item, productImage, formatCurrencyForDisplay, onViewAddonsClick)
            itemView.setOnClickListener {
                if (item.isVariation) {
                    productItemListener.openOrderProductVariationDetail(item.productId, item.variationId)
                } else {
                    productItemListener.openOrderProductDetail(item.productId)
                }
            }
        }
    }

    private inner class GroupedItemViewHolder(val binding: OrderDetailProductGroupItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @Suppress("MagicNumber")
        fun onBind(
            groupedItem: OrderProduct.GroupedProductItem,
            productImageMap: ProductImageMap,
            formatCurrencyForDisplay: (BigDecimal) -> String,
            productItemListener: OrderProductActionListener,
            onViewAddonsClick: ViewAddonClickListener? = null
        ) {
            val item = groupedItem.product
            val imageSize = itemView.resources.getDimensionPixelSize(R.dimen.image_major_50)
            val productImage = PhotonUtils.getPhotonImageUrl(productImageMap.get(item.uniqueId), imageSize, imageSize)

            binding.productInfoGroupedProduct.initView(item, productImage, formatCurrencyForDisplay, onViewAddonsClick)
            binding.productInfoGroupedProduct.setOnClickListener {
                if (item.isVariation) {
                    productItemListener.openOrderProductVariationDetail(item.productId, item.variationId)
                } else {
                    productItemListener.openOrderProductDetail(item.productId)
                }
            }
            binding.productInfoGroupedProduct.hideProductTotal()
            val itemTotal = formatCurrencyForDisplay(item.total)
            val productTotal = formatCurrencyForDisplay(groupedItem.groupedProductTotal)
            binding.groupedProductItemTotal.text = itemTotal
            binding.groupedProductTotal.text = productTotal

            binding.root.setOnClickListener {
                groupedItem.isExpanded = groupedItem.isExpanded.not()
                notifyItemChanged(bindingAdapterPosition)
            }

            if (groupedItem.isExpanded.not()) {
                binding.productInfoChildrenRecyclerView.isVisible = false
                binding.productInfoChildrenDivider.isVisible = false
                binding.expandIcon.rotation = 0f
                return
            } else {
                binding.productInfoChildrenRecyclerView.isVisible = true
                binding.productInfoChildrenDivider.isVisible = true
                binding.expandIcon.rotation = 180f
            }

            val childrenAdapter = OrderDetailProductChildItemListAdapter(
                groupedItem.children,
                productImageMap,
                formatCurrencyForDisplay,
                productItemListener
            )

            binding.productInfoChildrenRecyclerView.apply {
                setHasFixedSize(false)
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
                adapter = childrenAdapter

                // Setting this field to false ensures that the RecyclerView children do NOT receive multiple clicks,
                // and only processes the first click event. More details on this issue can be found here:
                // https://github.com/woocommerce/woocommerce-android/issues/2074
                isMotionEventSplittingEnabled = false
            }
        }
    }

    companion object {
        private const val PRODUCT_ITEM_VIEW = 1
        private const val GROUPED_PRODUCT_ITEM_VIEW = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (productItems[position]) {
            is OrderProduct.GroupedProductItem -> GROUPED_PRODUCT_ITEM_VIEW
            is OrderProduct.ProductItem -> PRODUCT_ITEM_VIEW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            GROUPED_PRODUCT_ITEM_VIEW -> {
                val binding = OrderDetailProductGroupItemBinding.inflate(layoutInflater, parent, false)
                GroupedItemViewHolder(binding)
            }

            PRODUCT_ITEM_VIEW -> {
                val view: OrderDetailProductItemView =
                    layoutInflater.inflate(R.layout.order_detail_product_list_item, parent, false)
                        as OrderDetailProductItemView
                ProductItemViewHolder(view)
            }

            else -> {
                // Fail fast if a new view type is added so we can handle it
                error("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            GROUPED_PRODUCT_ITEM_VIEW -> {
                (holder as GroupedItemViewHolder).onBind(
                    productItems[position] as OrderProduct.GroupedProductItem,
                    productImageMap,
                    formatCurrencyForDisplay,
                    productItemListener,
                    onViewAddonsClick
                )
            }

            PRODUCT_ITEM_VIEW -> {
                (holder as ProductItemViewHolder).onBind(
                    productItems[position] as OrderProduct.ProductItem,
                    productImageMap,
                    formatCurrencyForDisplay,
                    productItemListener,
                    onViewAddonsClick
                )
            }
        }
    }

    override fun getItemCount() = productItems.size
}
