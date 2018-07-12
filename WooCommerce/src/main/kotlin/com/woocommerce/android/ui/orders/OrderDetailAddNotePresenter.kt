package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.POST_ORDER_NOTE
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject

class OrderDetailAddNotePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderDetailAddNoteContract.Presenter {
    private var addNoteView: OrderDetailAddNoteContract.View? = null

    override fun takeView(view: OrderDetailAddNoteContract.View) {
        addNoteView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        addNoteView = null
        dispatcher.unregister(this)
    }

    override fun pushOrderNote(orderId: OrderIdentifier, noteText: String, isCustomerNote: Boolean) {
        val orderModel = orderStore.getOrderByIdentifier(orderId)
        if (orderModel == null) {
            addNoteView?.showNullOrderError()
            return
        }

        val noteModel = WCOrderNoteModel()
        noteModel.isCustomerNote = isCustomerNote
        noteModel.note = noteText

        val payload = WCOrderStore.PostOrderNotePayload(orderModel, selectedSite.get(), noteModel)
        dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))

        addNoteView?.doBeforeAddNote()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == POST_ORDER_NOTE) {
            val didSucceed = !event.isError
            addNoteView?.doAfterAddNote(didSucceed)
        }
    }
}
