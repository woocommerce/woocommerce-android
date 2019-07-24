package com.woocommerce.android.ui.orders.detail

import com.woocommerce.android.ui.orders.detail.OrderDetailPaymentViewModel.StaticUiState
import com.woocommerce.android.viewmodel.view.IMvvmViewState
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OrderDetailPaymentViewState(
    val uiState: StaticUiState? = null
) : IMvvmViewState
