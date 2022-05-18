package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreationRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class SimplePaymentsDialogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderUpdateStore: OrderUpdateStore,
    private val networkStatus: NetworkStatus,
    private val orderMapper: OrderMapper,
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData

    var currentPrice: BigDecimal
        get() = viewState.currentPrice
        set(value) {
            viewState = viewState.copy(
                currentPrice = value,
                isDoneButtonEnabled = value > BigDecimal.ZERO
            )
        }

    fun onDoneButtonClicked() {
        createSimplePaymentsOrder()
    }

    private fun createSimplePaymentsOrder() {
        if (!networkStatus.isConnected()) {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            return
        }

        viewState = viewState.copy(isProgressShowing = true, isDoneButtonEnabled = false)

        launch(Dispatchers.IO) {
            val status = if (isAutoDraftSupported()) {
                WCOrderStatusModel(statusKey = OrderCreationRepository.AUTO_DRAFT)
            } else {
                null
            }
            val result = orderUpdateStore.createSimplePayment(
                site = selectedSite.get(),
                amount = viewState.currentPrice.toString(),
                isTaxable = true,
                status = status
            )

            withContext(Dispatchers.Main) {
                viewState = viewState.copy(isProgressShowing = false, isDoneButtonEnabled = true)
                if (result.isError) {
                    WooLog.e(WooLog.T.ORDERS, "${result.error.type.name}: ${result.error.message}")
                    AnalyticsTracker.track(
                        AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_FAILED,
                        mapOf(KEY_SOURCE to VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT)
                    )
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.simple_payments_creation_error))
                } else {
                    viewState = viewState.copy(createdOrder = orderMapper.toAppModel(result.model!!))
                }
            }
        }
    }

    private suspend fun isAutoDraftSupported(): Boolean {
        val version = withContext(dispatchers.io) {
            wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version
                ?: "0.0"
        }
        return version.semverCompareTo(OrderCreationRepository.AUTO_DRAFT_SUPPORTED_VERSION) >= 0
    }

    @Parcelize
    data class ViewState(
        val currentPrice: BigDecimal = BigDecimal.ZERO,
        val isDoneButtonEnabled: Boolean = false,
        val isProgressShowing: Boolean = false,
        val createdOrder: Order? = null
    ) : Parcelable
}
