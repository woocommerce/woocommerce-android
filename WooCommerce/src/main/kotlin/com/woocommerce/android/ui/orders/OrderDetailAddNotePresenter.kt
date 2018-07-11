package com.woocommerce.android.ui.orders

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class OrderDetailAddNotePresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderDetailAddNoteContract.Presenter {
    companion object {
        private val TAG: String = OrderDetailAddNotePresenter::class.java.simpleName
    }

    private var addNoteView: OrderDetailAddNoteContract.View? = null

    override fun takeView(view: OrderDetailAddNoteContract.View) {
        addNoteView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        addNoteView = null
        dispatcher.unregister(this)
    }
}
