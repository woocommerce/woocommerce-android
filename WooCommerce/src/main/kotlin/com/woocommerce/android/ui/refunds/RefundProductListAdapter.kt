package com.woocommerce.android.ui.refunds

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
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal

class RefundProductListAdapter(
    private val formatCurrency: (BigDecimal) -> String,
    private val imageMap: ProductImageMap,
    private val isReadOnly: Boolean,
    private val onRefundQuantityClicked: (Long) -> Unit = { }
) : RecyclerView.Adapter<RefundProductListAdapter.ViewHolder>() {
    private var items = mutableListOf<RefundListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): ViewHolder {
        val layout = if (isReadOnly) R.layout.refunds_detail_product_list_item else R.layout.refunds_product_list_item
        return ViewHolder(parent, layout, formatCurrency, onRefundQuantityClicked, imageMap)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<RefundListItem>) {
        val diffResult = DiffUtil.calculateDiff(OrderItemDiffCallback(items, newItems))
        items = newItems.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

    class ViewHolder(
        parent: ViewGroup,
        @LayoutRes layout: Int,
        private val formatCurrency: (BigDecimal) -> String,
        private val onRefundQuantityClicked: (Long) -> Unit,
        private val imageMap: ProductImageMap
    ) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context).inflate(layout, parent, false)
    ) {
        private val nameTextView: TextView = itemView.findViewById(R.id.refundItem_productName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.refundItem_description)
        private val quantityTextView: TextView = itemView.findViewById(R.id.refundItem_quantity)
        private val productImageView: ImageView = itemView.findViewById(R.id.refundItem_icon)

        fun bind(item: RefundListItem) {
            nameTextView.text = item.orderItem.name

            descriptionTextView.text = itemView.context.getString(
                    R.string.order_refunds_item_description,
                    item.orderItem.quantity,
                    formatCurrency(item.orderItem.price)
            )

            quantityTextView.text = item.quantity.toString()
            quantityTextView.setOnClickListener {
                onRefundQuantityClicked(item.orderItem.uniqueId)
            }

            imageMap.get(item.orderItem.productId)?.let {
                val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
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
        val maxQuantity: Int = 0,
        val quantity: Int = 0
    ) : Parcelable {
        fun toDataModel(): WCRefundItem {
            return WCRefundItem(
                    orderItem.itemId,
                    quantity,
                    quantity.toBigDecimal().times(orderItem.price),
                    orderItem.totalTax.divide(orderItem.quantity.toBigDecimal()).times(quantity.toBigDecimal())
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
