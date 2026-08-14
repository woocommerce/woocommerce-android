package com.woocommerce.android.ui.payments.cardreader.readermode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.remote.CardReaderRemoteCertificateKeyType
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSession
import com.woocommerce.android.cardreader.remote.CardReaderRemoteSessionState
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayError
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionDenied
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayLocalNetworkPermissionExplainer
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class CardReaderModeViewModelTest : BaseUnitTest() {
    private val sessionState = MutableStateFlow<CardReaderRemoteSessionState>(CardReaderRemoteSessionState.Idle)
    private val session: CardReaderRemoteSession = mock {
        on { state }.thenReturn(sessionState)
        on { certificateKeyType }.thenReturn(CardReaderRemoteCertificateKeyType.ECDSA_256)
    }
    private val cardReaderManager: CardReaderManager = mock {
        on { initialized }.thenReturn(true)
    }
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(
            SiteModel().apply {
                siteId = 1L
                url = "https://example.com"
            }
        )
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { wooPosRemoteReaderDeviceUUID }.thenReturn("test-device-id")
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
                    analyticsTrackerWrapper,
                    selectedSite,
                    appPrefsWrapper,
                ) as T
        }
        viewModel = ViewModelProvider(store, factory)[CardReaderModeViewModel::class.java]
    }

    @Test
    fun `when view model initialized, then session is not started yet`() {
        // THEN
        verify(session, never()).start(any(), any(), any(), any())
    }

    @Test
    fun `given all permissions granted, when reported, then session started`() {
        // WHEN
        viewModel.onPermissionsGranted()

        // THEN
        verify(session).start(any(), any(), any(), any())
    }

    @Test
    fun `given all permissions granted twice, when reported, then session started only once`() {
        // WHEN
        viewModel.onPermissionsGranted()
        viewModel.onPermissionsGranted()

        // THEN
        verify(session, times(1)).start(any(), any(), any(), any())
    }

    @Test
    fun `when location permission is missing, then location explainer state is shown`() = testBlocking {
        // WHEN
        viewModel.onLocationPermissionMissing()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocationPermissionExplainer::class.java)
        verify(session, never()).start(any(), any(), any(), any())
    }

    @Test
    fun `when location permission is permanently denied, then location denied state is shown`() = testBlocking {
        // WHEN
        viewModel.onLocationPermissionDenied()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocationPermissionDenied::class.java)
        verify(session, never()).start(any(), any(), any(), any())
    }

    @Test
    fun `when local network permission is missing, then local network explainer state is shown`() = testBlocking {
        // WHEN
        viewModel.onLocalNetworkPermissionMissing()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocalNetworkPermissionExplainer::class.java)
        verify(session, never()).start(any(), any(), any(), any())
    }

    @Test
    fun `when local network permission is permanently denied, then local network denied state is shown`() =
        testBlocking {
            // WHEN
            viewModel.onLocalNetworkPermissionDenied()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayLocalNetworkPermissionDenied::class.java)
            verify(session, never()).start(any(), any(), any(), any())
        }

    @Test
    fun `given starting session state, when emitted, then starting view state is shown`() = testBlocking {
        // GIVEN
        viewModel.onPermissionsGranted()

        // WHEN
        sessionState.value = CardReaderRemoteSessionState.Starting
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value).isInstanceOf(RemoteTapToPayStarting::class.java)
    }

    @Test
    fun `given ready to pair session state, when emitted, then ready to pair view state is shown`() = testBlocking {
        // GIVEN
        viewModel.onPermissionsGranted()

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
            viewModel.onPermissionsGranted()

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
        viewModel.onPermissionsGranted()

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
        viewModel.onPermissionsGranted()
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

    @Test
    fun `given session transitions to ReadyToPair, when emitted, then session started event is tracked`() =
        testBlocking {
            // GIVEN
            viewModel.onPermissionsGranted()

            // WHEN
            sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
                deviceName = "Pixel",
                fingerprintSuffix = "1234",
            )
            advanceUntilIdle()

            // THEN
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_STARTED),
                eq(mapOf("is_simulated" to false, "certificate_key_type" to "ecdsa_256")),
            )
        }

    @Test
    fun `given session fell back to an rsa certificate, when started, then rsa key type is tracked`() =
        testBlocking {
            // GIVEN
            whenever(session.certificateKeyType).thenReturn(CardReaderRemoteCertificateKeyType.RSA_2048)
            viewModel.onPermissionsGranted()

            // WHEN
            sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
                deviceName = "Pixel",
                fingerprintSuffix = "1234",
            )
            advanceUntilIdle()

            // THEN
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_STARTED),
                eq(mapOf("is_simulated" to false, "certificate_key_type" to "rsa_2048")),
            )
        }

    @Test
    fun `given the session errors, when tracked, then the error description carries the cause chain`() =
        testBlocking {
            // GIVEN
            viewModel.onPermissionsGranted()

            // WHEN
            sessionState.value = CardReaderRemoteSessionState.Error(
                message = "java.lang.IllegalStateException: handshake failed",
                errorDescription = "java.lang.IllegalStateException: handshake failed" +
                    " <- caused by: java.security.cert.CertificateException: untrusted root",
            )
            advanceUntilIdle()

            // THEN
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ERROR),
                eq(
                    mapOf(
                        "error_description" to "java.lang.IllegalStateException: handshake failed" +
                            " <- caused by: java.security.cert.CertificateException: untrusted root"
                    )
                ),
            )
        }

    @Test
    fun `given a tablet connected then the session errored, when cleared, then error reason is reported`() =
        testBlocking {
            // GIVEN
            viewModel.onPermissionsGranted()
            sessionState.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = "iPad")
            advanceUntilIdle()
            sessionState.value = CardReaderRemoteSessionState.Error(message = "boom")
            advanceUntilIdle()

            // WHEN
            store.clear()

            // THEN
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ENDED),
                eq(
                    mapOf(
                        "reason" to "error",
                        "last_state" to "error",
                        "tablet_connected" to true,
                    )
                ),
            )
        }

    @Test
    fun `given no tablet ever connected, when cleared without an explicit exit, then dismissed is reported`() =
        testBlocking {
            // GIVEN
            viewModel.onPermissionsGranted()
            sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
                deviceName = "Pixel",
                fingerprintSuffix = "1234",
            )
            advanceUntilIdle()

            // WHEN
            store.clear()

            // THEN
            verify(analyticsTrackerWrapper).track(
                eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ENDED),
                eq(
                    mapOf(
                        "reason" to "dismissed",
                        "last_state" to "ready_to_pair",
                        "tablet_connected" to false,
                    )
                ),
            )
        }

    @Test
    fun `given the user taps the exit action, when cleared, then user exit reason is reported`() = testBlocking {
        // GIVEN
        viewModel.onPermissionsGranted()
        sessionState.value = CardReaderRemoteSessionState.ReadyToPair(
            deviceName = "Pixel",
            fingerprintSuffix = "1234",
        )
        advanceUntilIdle()
        (viewModel.viewState.value as RemoteTapToPayReadyToPair).onPrimaryActionClicked()

        // WHEN
        store.clear()

        // THEN
        verify(analyticsTrackerWrapper).track(
            eq(AnalyticsEvent.REMOTE_TTP_PHONE_SESSION_ENDED),
            eq(
                mapOf(
                    "reason" to "user_exit",
                    "last_state" to "ready_to_pair",
                    "tablet_connected" to false,
                )
            ),
        )
    }
}
