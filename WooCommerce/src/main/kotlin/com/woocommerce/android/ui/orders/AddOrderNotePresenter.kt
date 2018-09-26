package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class AddOrderNotePresenter @Inject constructor(
    private val orderStore: WCOrderStore
) : AddOrderNoteContract.Presenter {
    private var addNoteView: AddOrderNoteContract.View? = null

    override fun takeView(view: AddOrderNoteContract.View) {
        addNoteView = view
    }

    override fun dropView() {
        addNoteView = null
    }

    override fun hasBillingEmail(orderId: OrderIdentifier): Boolean {
        if (orderId.isNotEmpty()) {
            val email = orderStore.getOrderByIdentifier(orderId)?.billingEmail
            return email != null && email.isNotEmpty()
        }
        return false
    }
}
