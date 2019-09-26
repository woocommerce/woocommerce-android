package com.woocommerce.android.ui.refunds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_AMOUNT_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.BG_THREAD
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OpenClassOnDebug
class RefundDetailViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val refundStore: RefundsStore,
    private val selectedSite: SelectedSite,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(mainDispatcher) {
    private val _screenTitle = MutableLiveData<String>()
    val screenTitle: LiveData<String> = _screenTitle

    private val _formattedRefundAmount = MutableLiveData<String>()
    val formattedRefundAmount: LiveData<String> = _formattedRefundAmount

    private lateinit var formatCurrency: (BigDecimal) -> String

    fun start(orderId: Long, refundId: Long) {
    }
}
