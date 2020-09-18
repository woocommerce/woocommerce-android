package com.woocommerce.android.ui.orders.notes

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.notes.AddOrderNoteContract.Presenter
import com.woocommerce.android.ui.orders.notes.AddOrderNoteContract.View
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class AddOrderNotePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus
) : Presenter {
    private var addNoteView: View? = null

    override fun takeView(view: View) {
        addNoteView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        addNoteView = null
        dispatcher.unregister(this)
    }

    override fun hasBillingEmail(orderId: OrderIdentifier): Boolean {
        if (orderId.isNotEmpty()) {
            val email = orderStore.getOrderByIdentifier(orderId)?.billingEmail
            return email != null && email.isNotEmpty()
        }
        return false
    }

    override fun pushOrderNote(orderId: OrderIdentifier, noteText: String, isCustomerNote: Boolean): Boolean {
        if (!networkStatus.isConnected()) {
            addNoteView?.showOfflineSnack()
            return false
        }

        val order = orderStore.getOrderByIdentifier(orderId)
        if (order == null) {
            addNoteView?.showAddOrderNoteErrorSnack()
            return false
        }
        AnalyticsTracker.track(ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to order.remoteOrderId))

        val noteModel = WCOrderNoteModel()
        noteModel.isCustomerNote = isCustomerNote
        noteModel.note = noteText

        val payload = WCOrderStore.PostOrderNotePayload(order.id, order.remoteOrderId, selectedSite.get(), noteModel)
        dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))

        addNoteView?.showAddOrderNoteSnack()
        return true
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        // ignore this event as it will be handled by order detail - this is only here because we need
        // at least one @Subscribe in order to register with the dispatcher.
    }
}
