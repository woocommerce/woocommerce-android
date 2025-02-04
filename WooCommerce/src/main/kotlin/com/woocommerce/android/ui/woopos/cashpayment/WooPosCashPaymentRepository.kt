package com.woocommerce.android.ui.woopos.cashpayment

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.CurrencyFormattingParameters
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosCashPaymentRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val orderStore: WCOrderStore,
    private val orderMapper: OrderMapper,
    private val gatewayStore: WCGatewayStore,
) {

    private var cachedParameters: SiteParameters? = null

    suspend fun getOrderById(orderId: Long) = withContext(Dispatchers.IO) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let {
            orderMapper.toAppModel(it)
        }
    }

    suspend fun completeOrder(orderId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val codGateway = gatewayStore.getGateway(selectedSite.get(), CASH_ON_DELIVERY_PAYMENT_TYPE)

        val statusModel = orderStore.getOrderStatusForSiteAndKey(
            selectedSite.get(),
            Order.Status.Completed.value
        ) ?: WCOrderStatusModel(statusKey = Order.Status.Completed.value).apply {
            label = statusKey
        }

        orderStore.updateOrderStatusAndPaymentMethod(
            orderId = orderId,
            site = selectedSite.get(),
            newStatus = statusModel,
            newPaymentMethodId = CASH_ON_DELIVERY_PAYMENT_TYPE,
            newPaymentMethodTitle = codGateway?.title ?: "Pay in Person",
        )
            .filterIsInstance<WCOrderStore.UpdateOrderResult.RemoteUpdateResult>()
            .map { result ->
                if (result.event.isError) {
                    WooLog.e(T.POS, "Order completion failed - ${result.event.error.message}")
                    Result.failure(Exception(result.event.error.message))
                } else {
                    Result.success(Unit)
                }
            }.first()
    }

    suspend fun getCurrencySymbolPosition(): WCSettingsModel.CurrencyPosition {
        val params = getParams()
        val currencyFormattingParameters = params.currencyFormattingParameters
        return currencyFormattingParameters?.currencyPosition ?: WCSettingsModel.CurrencyPosition.LEFT
    }

    suspend fun getDecimalSeparator(): String {
        val params = getParams()
        val currencyFormattingParameters = params.currencyFormattingParameters
        return currencyFormattingParameters?.currencyDecimalSeparator ?: "."
    }

    suspend fun getNumberOfDecimals(): Int {
        val params = getParams()
        val currencyFormattingParameters = params.currencyFormattingParameters
        return currencyFormattingParameters?.currencyDecimalNumber ?: 2
    }

    suspend fun getCurrencySymbol(): String {
        return getParams().currencySymbol.orEmpty()
    }

    private suspend fun getParams(): SiteParameters {
        return cachedParameters ?: loadParameters().also {
            cachedParameters = it
        }
    }

    private suspend fun loadParameters(): SiteParameters = withContext(Dispatchers.IO) {
        val siteSettings = wooCommerceStore.getSiteSettings(selectedSite.get())
        val currencyCode = siteSettings?.currencyCode
        val currencySymbol = wooCommerceStore.getSiteCurrency(selectedSite.get(), currencyCode)
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get()).let {
            Pair(it?.weightUnit, it?.dimensionUnit)
        }
        val currencyFormattingParameters = siteSettings?.let {
            CurrencyFormattingParameters(
                currencyDecimalNumber = it.currencyDecimalNumber,
                currencyPosition = it.currencyPosition,
                currencyDecimalSeparator = it.currencyDecimalSeparator,
                currencyThousandSeparator = it.currencyThousandSeparator
            )
        }

        SiteParameters(
            currencyCode,
            currencySymbol,
            currencyFormattingParameters,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
    }

    private companion object {
        const val CASH_ON_DELIVERY_PAYMENT_TYPE = "cod"
    }
}
