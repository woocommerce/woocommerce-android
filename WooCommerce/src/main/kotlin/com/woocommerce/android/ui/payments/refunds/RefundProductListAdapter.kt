package com.woocommerce.android.ui.payments.refunds

import android.annotation.SuppressLint
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout.GONE
import androidx.constraintlayout.widget.ConstraintLayout.VISIBLE
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.payments.refunds.RefundProductListAdapter.RefundViewHolder
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.util.PhotonUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

typealias ViewAddonClickListener = (Order.Item) -> Unit

class RefundProductListAdapter(
    private val formatCurrency: (BigDecimal) -> String,
    private val imageMap: ProductImageMap,
    private val isProductDetailList: Boolean,
    private val onItemClicked: (Long) -> Unit = { },
    private val onViewAddonsClick: ViewAddonClickListener? = null
) : RecyclerView.Adapter<RefundViewHolder>() {
    private var items = mutableListOf<ProductRefundListItem>()

    override fun onCreateViewHolder(parent: ViewGroup, itemType: Int): RefundViewHolder {
        return if (isProductDetailList) {
            RefundDetailViewHolder(parent, formatCurrency, imageMap, onViewAddonsClick)
        } else {
            IssueRefundViewHolder(parent, formatCurrency, onItemClicked, imageMap)
        }
    }

    override fun onBindViewHolder(holder: RefundViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<ProductRefundListItem>) {
        val diffResult = DiffUtil.calculateDiff(OrderItemDiffCallback(items, newItems))
        items = newItems.toMutableList()
        diffResult.dispatchUpdatesTo(this)
    }

    abstract class RefundViewHolder(parent: ViewGroup, @LayoutRes layout: Int) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(layout, parent, false)
    ) {
        abstract fun bind(item: ProductRefundListItem)
    }

    class RefundDetailViewHolder(
        parent: ViewGroup,
        private val formatCurrency: (BigDecimal) -> String,
        private val imageMap: ProductImageMap,
        private val onViewAddonsClick: ViewAddonClickListener?
    ) : RefundViewHolder(parent, R.layout.refunds_detail_product_list_item) {
        private val nameTextView: TextView = itemView.findViewById(R.id.refundItem_productName)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.refundItem_description)
        private val skuTextView: TextView = itemView.findViewById(R.id.refundItem_sku)
        private val quantityTextView: TextView = itemView.findViewById(R.id.refundItem_quantity)
        private val productImageView: ImageView = itemView.findViewById(R.id.refundItem_icon)
        private val productAddonsView: TextView = itemView.findViewById(R.id.refundItem_addons)

        @SuppressLint("SetTextI18n")
        override fun bind(item: ProductRefundListItem) {
            nameTextView.text = item.orderItem.name

            if (item.orderItem.sku.isBlank()) {
                skuTextView.hide()
            } else {
                skuTextView.text = "SKU: ${item.orderItem.sku}"
                skuTextView.show()
            }

            val totalRefund = formatCurrency(item.orderItem.price.times(item.quantity.toBigDecimal()))
            if (item.quantity > 1) {
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

            productAddonsView.visibility =
                if (item.orderItem.containsAddons && AppPrefs.isProductAddonsEnabled) {
                    VISIBLE
                } else {
                    GONE
                }

            productAddonsView.setOnClickListener {
                onViewAddonsClick?.invoke(item.orderItem)
            }

            imageMap.get(item.orderItem.productId)?.let {
                val imageCornerRadius = itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
                val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
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
        private val subtotalTextView: TextView = itemView.findViewById(R.id.refundItemSubtotal)
        private val taxesTextView: TextView = itemView.findViewById(R.id.refundItemTaxes)

        @SuppressLint("SetTextI18n")
        override fun bind(item: ProductRefundListItem) {
            nameTextView.text = item.orderItem.name

            descriptionTextView.text = itemView.context.getString(
                R.string.order_refunds_item_description,
                item.maxQuantity.formatToString(),
                formatCurrency(item.orderItem.price)
            )

            quantityTextView.text = item.quantity.toString()
            quantityTextView.setOnClickListener {
                onItemClicked(item.orderItem.itemId)
            }

            imageMap.get(item.orderItem.productId)?.let {
                val imageCornerRadius = itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
                val imageSize = itemView.context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
                    .placeholder(R.drawable.ic_product)
                    .into(productImageView)
            } ?: productImageView.setImageResource(R.drawable.ic_product)

            subtotalTextView.text = item.subtotal

            taxesTextView.text = item.taxes
        }
    }

    @Parcelize
    data class ProductRefundListItem(
        val orderItem: Order.Item,
        val maxQuantity: Float = 0f,
        val quantity: Int = 0,
        val subtotal: String? = null,
        val taxes: String? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val availableRefundQuantity
            get() = maxQuantity.toInt()
        fun toDataModel(): WCRefundItem {
            return WCRefundItem(
                orderItem.itemId,
                quantity,
                quantity.toBigDecimal().times(orderItem.price),
                orderItem.totalTax.divide(orderItem.quantity.toBigDecimal(), 2, HALF_UP)
                    .times(quantity.toBigDecimal())
            )
        }
    }

    class OrderItemDiffCallback(
        private val oldList: List<ProductRefundListItem>,
        private val newList: List<ProductRefundListItem>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].orderItem.itemId == newList[newItemPosition].orderItem.itemId
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
