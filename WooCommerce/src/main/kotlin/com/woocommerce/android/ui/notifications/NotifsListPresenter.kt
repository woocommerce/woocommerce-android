package com.woocommerce.android.ui.notifications

import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.notifications.NotifsListContract.View
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore
import javax.inject.Inject

class NotifsListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val networkStatus: NetworkStatus
) : NotifsListContract.Presenter {
    companion object {
        private val TAG: String = NotifsListPresenter::class.java.simpleName
    }

    private var view: NotifsListContract.View? = null
    private var isLoading = false
    private var isLoadingMore = false
    private var canLoadMore = false

    override fun takeView(view: View) {
        this.view = view
        ConnectionChangeReceiver.getEventBus().register(this)

        // TODO - register dispatcher
    }

    override fun dropView() {
        view = null
        ConnectionChangeReceiver.getEventBus().unregister(this)

        // TODO - unregister dispatcher
    }

    override fun loadNotifs(forceRefresh: Boolean) {
        if (networkStatus.isConnected() && forceRefresh) {
            isLoading = true
            view?.showSkeleton(true)

            // TODO add real data here
            val notifs = listOf(
                    WCNotificationModel.Order(1, "You have a new order!",
                            "Amanda test placed a $9.00 order from Candle Kingdom.", "2018-10-22T21:08:11+00:00", "1"),
                    WCNotificationModel.Review(7, "Joe Smith left a review", "Review for Eyes Wide Shut",
                            "2018-10-22T21:08:11+00:00", 2, 1F, ""),
                    WCNotificationModel.Review(7, "Yuval Noah Harari left a review",
                            "Review for Sapiens: A Brief History of Humankind", "2018-7-22T21:08:11+00:00", 5, 5F, ""),
                    WCNotificationModel.Review(7, "Yuval Noah Harari left a review",
                            "Review for Homo Deus: A Brief History of tomorrow", "2018-9-22T21:08:11+00:00", 4, 4F, ""),
                    WCNotificationModel.Review(7, "Gillian Flynn left a review", "Review for Sharp Objects",
                            "2018-11-1T21:08:11+00:00", 2, 3.5F, "")
            )
            view?.let {
                it.showNotifications(notifs, true)
            }
        } else {
            // Load cached notifications from the db
        }
    }

    override fun loadMoreNotifs() {
        if (!networkStatus.isConnected()) return

        TODO("not implemented")
    }

    override fun canLoadMore() = canLoadMore

    override fun isLoading() = isLoading || isLoadingMore

    override fun fetchAndLoadNotifsFromDb(isForceRefresh: Boolean) {
        TODO("not implemented")
    }

    override fun setNotifsSeen() {
        TODO("not implemented")
    }

    override fun setNotificationRead() {
        TODO("not implemented")
    }

    override fun setAllNotifsRead() {
        TODO("not implemented")
    }

    override fun getOrder(orderId: OrderIdentifier) = orderStore.getOrderByIdentifier(orderId)

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            // TODO refresh notifications if needed
        }
    }
}
