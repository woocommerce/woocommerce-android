package com.woocommerce.android.ui.orders

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.cardreader.CardReaderPaymentCollectibilityChecker
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.util.BaseJunit5Test
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class OrderDetailViewModelTestJUnit5 : BaseJunit5Test() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    private val appPrefsWrapper: AppPrefs = mock {
        on(it.isTrackingExtensionAvailable()).thenAnswer { true }
        on(it.isCardReaderOnboardingCompleted(anyInt(), anyLong(), anyLong())).thenAnswer { true }
    }
    private val resources: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val dispatcher: Dispatcher = mock()
    private val savedState = OrderDetailFragmentArgs(orderId = ORDER_IDENTIFIER).initSavedStateHandle()

    @Nested
    @DisplayName("Given a site with orders")
    inner class OrdersAvailable {
        val site = SiteModel().let {
            it.id = 1
            it.siteId = 1
            it.selfHostedSiteId = 1
            it
        }
        private val selectedSite: SelectedSite = mock {
            whenever(it.getIfExists()).thenReturn(site)
            whenever(it.get()).thenReturn(site)
        }
        private val order = OrderTestUtils.generateTestOrder(ORDER_IDENTIFIER)
        private val repository: OrderDetailRepository = mock {
            whenever(it.getOrder(any())).thenReturn(order)
            testBlocking {
                whenever(it.fetchOrder(any())).thenReturn(order)
                whenever(it.fetchOrderNotes(any(), any())).thenReturn(true)
            }
            whenever(it.getWooServicesPluginInfo()).thenReturn(
                WooPlugin(true, true, version = OrderDetailViewModel.SUPPORTED_WCS_VERSION)
            )
        }
        private val addonsRepository: AddonRepository = mock {
            testBlocking {
                whenever(it.containsAddonsFrom(any())).thenReturn(false)
            }
        }
        private val paymentCollectibilityChecker: CardReaderPaymentCollectibilityChecker = mock()
        private val networkStatus: NetworkStatus = mock {
            whenever(it.isConnected()).thenReturn(true)
        }

        private val viewModel = OrderDetailViewModel(
            dispatcher,
            coroutinesTestExtension.testDispatchers,
            savedState,
            appPrefsWrapper,
            networkStatus,
            resources,
            repository,
            addonsRepository,
            selectedSite,
            paymentCollectibilityChecker
        )
        private val currentViewStateValue
            get() = viewModel.viewStateData.liveData.value

        @Nested
        @DisplayName("When payment is collectable")
        inner class PaymentCollectable {
            @BeforeEach
            fun whenCondition() {
                whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(true)
                viewModel.start()
            }

            @Test
            @DisplayName("Collect button is shown")
            fun thenCondition() {
                assertTrue(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader)
            }
        }

        @Nested
        @DisplayName("When payment is not collectable")
        inner class PaymentNonCollectable {
            @BeforeEach
            fun whenCondition() {
                whenever(paymentCollectibilityChecker.isCollectable(any())).thenReturn(false)
                viewModel.start()
            }

            @Test
            @DisplayName("Collect button is hidden")
            fun thenCondition() {
                assertFalse(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader)
            }
        }
    }
//    @Test
//    fun `collect button hidden if payment is not collectable`() =
//        testBlocking {
//            // GIVEN
//            doReturn(false).whenever(paymentCollectibilityChecker).isCollectable(any())
//            doReturn(order).whenever(repository).getOrder(any())
//            doReturn(order).whenever(repository).fetchOrder(any())
//            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
//            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
//
//            // WHEN
//            viewModel.start()
//
//            // THEN
//            assertThat(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader).isFalse()
//        }
//
//    @Test
//    fun `collect button shown if payment is collectable`() =
//        testBlocking {
//            // GIVEN
//            doReturn(true).whenever(paymentCollectibilityChecker).isCollectable(any())
//            doReturn(order).whenever(repository).getOrder(any())
//            doReturn(order).whenever(repository).fetchOrder(any())
//            doReturn(true).whenever(repository).fetchOrderNotes(any(), any())
//            doReturn(false).whenever(addonsRepository).containsAddonsFrom(any())
//
//            // WHEN
//            viewModel.start()
//
//            // THEN
//            assertThat(currentViewStateValue!!.orderInfo!!.isPaymentCollectableWithCardReader).isTrue()
//        }
}
