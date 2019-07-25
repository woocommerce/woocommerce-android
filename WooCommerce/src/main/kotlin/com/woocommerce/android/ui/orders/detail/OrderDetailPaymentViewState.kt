package com.woocommerce.android.ui.orders.detail

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OrderDetailPaymentViewState(
    val title: String,
    val subtotal: String,
    val shippingTotal: String,
    val taxesTotal: String,
    val total: String,
    val refundTotal: String,
    val isPaymentMessageVisible: Boolean,
    val paymentMessage: String,
    val isRefundSectionVisible: Boolean,
    val totalAfterRefunds: String,
    val isDiscountSectionVisible: Boolean,
    val discountTotal: String,
    val discountItems: String
) : Parcelable
