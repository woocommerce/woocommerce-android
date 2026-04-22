package com.woocommerce.android.ui.ai.model

import androidx.annotation.ColorRes
import com.woocommerce.android.R

sealed interface MessageContent {
    data class Text(val value: String) : MessageContent
    data class OrderCards(val orders: List<OrderCardData>) : MessageContent
    data class ProductCards(val products: List<ProductCardData>) : MessageContent
}

data class OrderCardData(
    val id: Long,
    val number: String,
    val status: String,
    @ColorRes val statusColor: Int,
    val customerName: String,
    val totalPrice: String,
    val date: String
)

data class ProductCardData(
    val id: Long,
    val name: String,
    val price: String,
    val status: String,
    @ColorRes val statusColor: Int,
    val stockStatus: String,
    val imageUrl: String?
) {
    companion object {
        @ColorRes
        fun statusToColorRes(status: String): Int {
            return when (status.lowercase().trim()) {
                "publish", "published" -> R.color.tag_bg_completed
                "draft" -> R.color.tag_bg_other
                "pending" -> R.color.tag_bg_on_hold
                "private" -> R.color.tag_bg_processing
                else -> R.color.tag_bg_other
            }
        }

        @ColorRes
        fun orderStatusToColorRes(status: String): Int {
            return when (status.lowercase().trim()) {
                "completed" -> R.color.tag_bg_completed
                "processing" -> R.color.tag_bg_processing
                "on-hold", "on_hold" -> R.color.tag_bg_on_hold
                "failed", "cancelled" -> R.color.tag_bg_failed
                else -> R.color.tag_bg_other
            }
        }
    }
}
