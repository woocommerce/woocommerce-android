package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocationPermissionExplainer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayStarting
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class CardReaderModeViewModelTest : BaseUnitTest() {
    private val sessionState = MutableStateFlow<CardReaderRemoteSessionState>(CardReaderRemoteSessionState.Idle)
    private val session: CardReaderRemoteSession = mock {
        on { state }.thenReturn(sessionState)
    }
    private val cardReaderManager: CardReaderManager = mock {
        on { initialized }.thenReturn(true)
    }
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(
            SiteModel().apply {
                siteId = 1L
                url = "https://example.com"
            }
        )
    }

    private lateinit var store: ViewModelStore
    private lateinit var viewModel: CardReaderModeViewModel

    @Before
    fun setUp() {
        store = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CardReaderModeViewModel(
                    session,
                    cardReaderManager,
                    developerOptionsRepository,
                    selectedSite,
                ) as T
        }
        viewModel = ViewModelProvider(store, factory)[CardReaderModeViewModel::class.java]
    }

    @Test
    fun `when view model initialized, then session is not started yet`() {
        // THEN
        verify(session, never()).start(any(), any(), any())
    }

    @Test
    fun `given location permission granted, when result received, then session started`() {
        // WHEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

        // THEN
        verify(session).start(any(), any(), any())
    }

    @Test
    fun `given location permission granted twice, when received, then session started only once`() {
        // WHEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

        // THEN
        verify(session, times(1)).start(any(), any(), any())
    }

    @Test
    fun `given denied with rationale, when result received, then explainer state is shown`() = testBlocking {
        // WHEN
        viewModel.onLocationPermissionResult(granted = false, shouldShowRationale = true)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocationPermissionExplainer::class.java)
        verify(session, never()).start(any(), any(), any())
    }

    @Test
    fun `given permanently denied, when result received, then permission denied state is shown`() = testBlocking {
        // WHEN
        viewModel.onLocationPermissionResult(granted = false, shouldShowRationale = false)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocationPermissionDenied::class.java)
        verify(session, never()).start(any(), any(), any())
    }

    @Test
    fun `given starting session state, when emitted, then starting view state is shown`() = testBlocking {
        // GIVEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

        // WHEN
        sessionState.value = CardReaderRemoteSessionState.Starting
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayStarting::class.java)
    }

    @Test
    fun `given ready to pair session state, when emitted, then ready to pair view state is shown`() = testBlocking {
        // GIVEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

        // WHEN
        sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
            deviceName = "Pixel",
            fingerprintSuffix = "1234"
        )
        advanceUntilIdle()

        // THEN
        val viewState = viewModel.viewState.value as RemoteTapToPayReadyToPair
        assertThat(viewState.deviceName).isEqualTo("Pixel")
        assertThat(viewState.fingerprintSuffix).isEqualTo("1234")
        assertThat(viewState.siteUrl).isEqualTo("example.com")
    }

    @Test
    fun `given waiting for payment session state, when emitted, then waiting for payment view state is shown`() =
        testBlocking {
            // GIVEN
            viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

            // WHEN
            sessionState.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = "Tablet 1")
            advanceUntilIdle()

            // THEN
            val viewState = viewModel.viewState.value as RemoteTapToPayWaitingForPayment
            assertThat(viewState.tabletName).isEqualTo("Tablet 1")
        }

    @Test
    fun `given error session state, when emitted, then error view state carries the message`() = testBlocking {
        // GIVEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)

        // WHEN
        sessionState.value = CardReaderRemoteSessionState.Error(message = "java.net.SocketException: closed")
        advanceUntilIdle()

        // THEN
        val viewState = viewModel.viewState.value as RemoteTapToPayError
        assertThat(viewState.message).isEqualTo("java.net.SocketException: closed")
    }

    @Test
    fun `given starting view state, when cancel clicked, then exit event is emitted`() = testBlocking {
        // GIVEN
        viewModel.onLocationPermissionResult(granted = true, shouldShowRationale = false)
        sessionState.value = CardReaderRemoteSessionState.Starting
        advanceUntilIdle()

        // WHEN
        (viewModel.viewState.value as RemoteTapToPayStarting).onPrimaryActionClicked.invoke()

        // THEN
        assertThat(viewModel.events.first()).isEqualTo(CardReaderModeEvent.Exit)
    }

    @Test
    fun `when view model store cleared, then session is stopped`() {
        // WHEN
        store.clear()

        // THEN
        verify(session).stop()
    }
}
