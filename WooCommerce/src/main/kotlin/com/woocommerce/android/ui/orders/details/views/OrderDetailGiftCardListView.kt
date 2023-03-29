package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailGiftCardListBinding
import com.woocommerce.android.model.GiftCard
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailGiftCardListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.AlignedDividerDecoration

class OrderDetailGiftCardListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailGiftCardListBinding.inflate(LayoutInflater.from(ctx), this)
    fun updateGiftCardList(
        giftCards: List<GiftCard>,
        currencyFormatter: CurrencyFormatter,
        currencyCode: String
    ) {
        val giftCardsAdapter =
            binding.giftCardsItems.adapter as? OrderDetailGiftCardListAdapter
                ?: OrderDetailGiftCardListAdapter(currencyFormatter, currencyCode).also {
                    binding.giftCardsItems.apply {
                        setHasFixedSize(true)
                        layoutManager = LinearLayoutManager(context)
                        itemAnimator = DefaultItemAnimator()
                        adapter = it
                        if (itemDecorationCount == 0) {
                            addItemDecoration(
                                AlignedDividerDecoration(
                                    context,
                                    DividerItemDecoration.VERTICAL,
                                    R.id.gift_card_code
                                )
                            )
                        }
                    }
                }
        giftCardsAdapter.giftCardList = giftCards
    }
}
