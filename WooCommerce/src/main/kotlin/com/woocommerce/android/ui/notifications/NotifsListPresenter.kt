package com.woocommerce.android.ui.notifications

import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.notifications.NotifsListContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import javax.inject.Inject

class NotifsListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val notificationStore: NotificationStore,
    private val networkStatus: NetworkStatus
) : NotifsListContract.Presenter {
    companion object {
        private val TAG: String = NotifsListPresenter::class.java.simpleName
    }

    override var isLoading = false
    private var view: NotifsListContract.View? = null
    private var isRefreshing = false

    override fun takeView(view: View) {
        this.view = view
        ConnectionChangeReceiver.getEventBus().register(this)
        dispatcher.register(this)
    }

    override fun dropView() {
        view = null
        ConnectionChangeReceiver.getEventBus().unregister(this)
        dispatcher.unregister(this)
    }

    override fun loadNotifs(forceRefresh: Boolean) {
        view?.let {
            if (networkStatus.isConnected() && forceRefresh) {
                it.showSkeleton(true)
                isLoading = true
                isRefreshing = forceRefresh

                val payload = FetchNotificationsPayload()
                dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(payload))
            } else {
                // Load cached notifications from the db
                fetchAndLoadNotifsFromDb(forceRefresh)
            }
        }
    }

    override fun fetchAndLoadNotifsFromDb(isForceRefresh: Boolean) {
        view?.let { notifView ->
            val notifs = notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                    filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
            notifView.showNotifications(notifs, isFreshData = isForceRefresh)

            // TODO how to handle empty notifications list view?
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            view?.let { notifView ->
                if (notifView.isRefreshPending) {
                    notifView.refreshFragmentState()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        view?.showSkeleton(false)
        when (event.causeOfChange) {
            NotificationAction.FETCH_NOTIFICATIONS -> {
                if (event.isError) {
                    // TODO - track event

                    WooLog.e(NOTIFICATIONS, "$TAG - Error fetching notifications: ${event.error.message}")
                    view?.showLoadNotificationsError()
                    fetchAndLoadNotifsFromDb(false)
                } else {
                    // TODO - track event
                    val forceRefresh = isRefreshing
                    fetchAndLoadNotifsFromDb(forceRefresh)
                }
                isLoading = false
                isRefreshing = false
            }
            else -> {}
        }
    }
}
