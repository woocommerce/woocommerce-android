package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosReaderReadyForPaymentTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosTotalsAnalyticsTrackerTest {
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val analyticsData = WooPosAnalyticsTrackingDataKeeper()
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val readerReadyForPaymentTracker: WooPosReaderReadyForPaymentTracker = mock()

    private val tracker = WooPosTotalsAnalyticsTracker(
        analyticsTracker = analyticsTracker,
        analyticsData = analyticsData,
        productsDataSource = productsDataSource,
        readerReadyForPaymentTracker = readerReadyForPaymentTracker,
    )

    @Test
    fun `given a payment in progress, when it reaches processing, then the card tapped timestamp is stamped`() =
        runTest {
            // GIVEN
            val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(
                CardReaderPaymentState.LoadingData(onCancel = {})
            )
            val collection = launch(UnconfinedTestDispatcher(testScheduler)) {
                tracker.trackPaymentStates(paymentState)
            }
            assertThat(analyticsData.cardTappedTimestamp).isNull()

            // WHEN
            paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment(
                amountWithCurrencyLabel = "$1.00",
                onCancel = {},
            )

            // THEN
            assertThat(analyticsData.cardTappedTimestamp).isNotNull()
            collection.cancel()
        }
}
