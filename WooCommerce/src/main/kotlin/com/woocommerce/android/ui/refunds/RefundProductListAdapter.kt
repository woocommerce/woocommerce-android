package com.woocommerce.android.ui.refunds

import android.annotation.SuppressLint
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundViewHolder
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

class RefundProductListAdapter(
    private val formatCurrency: (BigDecimal) -> String,
    private val imageMap: ProductImageMap,
    private val isProductDetailList: Boolean,
    private val onItemClicked: (Long) -> Unit = { }
) : RecyclerView.Adapter<RefundViewHolder>() {
    private var items = mutableListOf<RefundListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): RefundViewHolder {
        return if (isProductDetailList)
            RefundDetailViewHolder(parent, formatCurrency, imageMap)
        else
            IssueRefundViewHolder(parent, formatCurrency, onItemClicked, imageMap)
    }

    override fun onBindViewHolder(holder: RefundViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<RefundListItem>) {
        val diffResult = DiffUtil.calculateDiff(OrderItemDiffCallback(items, newItems))
        items = newItems.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

    abstract class RefundViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
    ) {
        abstract fun bind(item: RefundListItem)
    }

    class RefundDetailViewHolder(
        parent: ViewGroup,
        private val formatCurrency: (BigDecimal) -> String,
        private val imageMap: ProductImageMap
    ) : RefundViewHolder(parent, R.layout.refunds_detail_product_list_item) {
        private val nameTextView: TextView = itemView.findViewById(R.id.refundItem_productName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.refundItem_description)
        private val skuTextView: TextView = itemView.findViewById(R.id.refundItem_sku)
        private val quantityTextView: TextView = itemView.findViewById(R.id.refundItem_quantity)
        private val productImageView: ImageView = itemView.findViewById(R.id.refundItem_icon)

        @SuppressLint("SetTextI18n")
        override fun bind(item: RefundListItem) {
            nameTextView.text = item.orderItem.name

            if (item.orderItem.sku.isBlank()) {
                skuTextView.hide()
            } else {
                skuTextView.text = "SKU: ${item.orderItem.sku}"
                skuTextView.show()
            }

            val totalRefund = formatCurrency(item.orderItem.price.times(item.quantity))
            if (item.quantity > BigDecimal.ONE) {
                descriptionTextView.text = itemView.context.getString(
                        R.string.order_refunds_detail_item_description,
                        totalRefund,
                        formatCurrency(item.orderItem.price),
                        item.quantity
                )
            } else {
                descriptionTextView.text = totalRefund
            }

            quantityTextView.text = item.quantity.toString()

            imageMap.get(item.orderItem.productId)?.let {
                val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                GlideApp.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_product)
                        .into(productImageView)
            } ?: productImageView.setImageResource(R.drawable.ic_product)
        }
    }

    class IssueRefundViewHolder(
        parent: ViewGroup,
        private val formatCurrency: (BigDecimal) -> String,
        private val onItemClicked: (Long) -> Unit,
        private val imageMap: ProductImageMap
    ) : RefundViewHolder(parent, R.layout.refunds_product_list_item) {
        private val nameTextView: TextView = itemView.findViewById(R.id.refundItem_productName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.refundItem_description)
        private val quantityTextView: TextView = itemView.findViewById(R.id.refundItem_quantity)
        private val productImageView: ImageView = itemView.findViewById(R.id.refundItem_icon)

        @SuppressLint("SetTextI18n")
        override fun bind(item: RefundListItem) {
            nameTextView.text = item.orderItem.name

            descriptionTextView.text = itemView.context.getString(
                    R.string.order_refunds_item_description,
                    item.maxQuantity,
                    formatCurrency(item.orderItem.price)
            )

            quantityTextView.text = item.quantity.toString()
            quantityTextView.setOnClickListener {
                onItemClicked(item.orderItem.uniqueId)
            }

            imageMap.get(item.orderItem.productId)?.let {
                val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                GlideApp.with(itemView.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_product)
                        .into(productImageView)
            } ?: productImageView.setImageResource(R.drawable.ic_product)
        }
    }

    @Parcelize
    data class RefundListItem(
        val orderItem: Order.Item,
        val maxQuantity: BigDecimal = BigDecimal.ZERO,
        val quantity: BigDecimal = BigDecimal.ZERO
    ) : Parcelable {
        fun toDataModel(): WCRefundItem {
            // TODO Change WCRefundItem in WordPress-FluxC-Android
            return WCRefundItem(
                    orderItem.itemId,
                    quantity.toInt(),
                    quantity.times(orderItem.price),
                    orderItem.totalTax.divide(orderItem.quantity, 2, HALF_UP)
                            .times(quantity)
            )
        }
    }

    class OrderItemDiffCallback(
        private val oldList: List<RefundListItem>,
        private val newList: List<RefundListItem>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].orderItem.uniqueId == newList[newItemPosition].orderItem.uniqueId
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old.orderItem.name == old.orderItem.name &&
                    old.orderItem.price isEqualTo new.orderItem.price &&
                    old.orderItem.quantity == new.orderItem.quantity &&
                    old.quantity == new.quantity &&
                    old.maxQuantity == new.maxQuantity
        }
    }
}
