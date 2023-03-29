package com.woocommerce.android.ui.orders.details.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailGiftCardListItemBinding
import com.woocommerce.android.model.GiftCard
import com.woocommerce.android.util.CurrencyFormatter

class OrderDetailGiftCardListAdapter(
    private val currencyFormatter: CurrencyFormatter,
    private val currencyCode: String
) :
    RecyclerView.Adapter<OrderDetailGiftCardListAdapter.OrderDetailGiftCardViewHolder>() {
    var giftCardList: List<GiftCard> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                GiftCardDiffCallback(
                    field,
                    value
                ),
                true
            )
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailGiftCardViewHolder {
        val viewBinding = OrderDetailGiftCardListItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderDetailGiftCardViewHolder(viewBinding, currencyFormatter, currencyCode)
    }

    override fun onBindViewHolder(holder: OrderDetailGiftCardViewHolder, position: Int) {
        holder.bind(giftCardList[position])
    }

    override fun getItemCount(): Int = giftCardList.size

    class OrderDetailGiftCardViewHolder(
        private val viewBinding: OrderDetailGiftCardListItemBinding,
        private val currencyFormatter: CurrencyFormatter,
        private val currencyCode: String
    ) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(giftCard: GiftCard) {
            viewBinding.giftCardCode.text = giftCard.code
            with(viewBinding.giftCardUsed) {
                text = context.getString(
                    R.string.gift_card_used,
                    currencyFormatter.formatCurrency(
                        amount = giftCard.used,
                        currencyCode = currencyCode,
                        applyDecimalFormatting = true
                    )
                )
            }
        }
    }

    class GiftCardDiffCallback(
        private val oldList: List<GiftCard>,
        private val newList: List<GiftCard>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }
    }
}
