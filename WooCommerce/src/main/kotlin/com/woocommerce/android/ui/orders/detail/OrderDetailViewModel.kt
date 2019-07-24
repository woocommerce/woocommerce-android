package com.woocommerce.android.ui.orders.detail

import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.order.Order
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject
import javax.inject.Named

class OrderDetailViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    val paymentInfoViewModel: OrderDetailPaymentViewModel
) : ScopedViewModel(mainDispatcher) {
    fun updatePaymentInfo(order: Order) {
        paymentInfoViewModel.update(order)
    }
}
