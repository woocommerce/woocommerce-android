package com.woocommerce.android.ui.woopos.cardreader.connection

import app.cash.turbine.test
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.WooPosPermissionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosCardReaderConnectionViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val controller: WooPosCardReaderConnectionController = mock()
    private val controllerFactory: WooPosCardReaderConnectionControllerFactory = mock {
        on { create(any()) }.thenReturn(controller)
    }
    private val permissionUtils: WooPosPermissionUtils = mock()

    private val controllerStateFlow = MutableStateFlow<WooPosCardReaderConnectionState>(
        WooPosCardReaderConnectionState.Scanning(isRemoteTapToPaySupported = false)
    )
    private val controllerEventFlow = MutableSharedFlow<WooPosCardReaderConnectionController.ControllerEvent>()

    @Test
    fun `given controller emits RequestBluetoothPermission, when event collected, then emits RequestBluetoothPermission`() =
        runTest {
            // GIVEN
            setupControllerMocks()
            val viewModel = createViewModel()

            // WHEN
            viewModel.event.test {
                controllerEventFlow.emit(
                    WooPosCardReaderConnectionController.ControllerEvent.RequestBluetoothPermission
                )

                // THEN
                assertThat(awaitItem()).isEqualTo(
                    WooPosCardReaderConnectionViewModel.Event.RequestBluetoothPermission
                )
            }
        }

    @Test
    fun `given controller emits RequestLocationPermission, when event collected, then emits RequestLocationPermission`() =
        runTest {
            // GIVEN
            setupControllerMocks()
            val viewModel = createViewModel()

            // WHEN
            viewModel.event.test {
                controllerEventFlow.emit(
                    WooPosCardReaderConnectionController.ControllerEvent.RequestLocationPermission
                )

                // THEN
                assertThat(awaitItem()).isEqualTo(
                    WooPosCardReaderConnectionViewModel.Event.RequestLocationPermission
                )
            }
        }

    @Test
    fun `given controller emits Cancelled, when event collected, then emits Dismissed`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.event.test {
            controllerEventFlow.emit(WooPosCardReaderConnectionController.ControllerEvent.Cancelled)

            // THEN
            assertThat(awaitItem()).isEqualTo(WooPosCardReaderConnectionViewModel.Event.Dismissed)
        }
    }

    @Test
    fun `given controller emits OpenAppSettings, when event collected, then calls permissionUtils showAppSettings`() =
        runTest {
            // GIVEN
            setupControllerMocks()
            createViewModel()

            // WHEN
            controllerEventFlow.emit(WooPosCardReaderConnectionController.ControllerEvent.OpenAppSettings)
            advanceUntilIdle()

            // THEN
            verify(permissionUtils).showAppSettings()
        }

    @Test
    fun `given controller emits OnboardingRequired, when event collected, then emits NavigateToOnboarding`() =
        runTest {
            // GIVEN
            setupControllerMocks()
            val viewModel = createViewModel()
            val onboardingState = CardReaderOnboardingState.GenericError

            // WHEN
            viewModel.event.test {
                controllerEventFlow.emit(
                    WooPosCardReaderConnectionController.ControllerEvent.OnboardingRequired(onboardingState)
                )

                // THEN
                val event = awaitItem()
                assertThat(event).isInstanceOf(
                    WooPosCardReaderConnectionViewModel.Event.NavigateToOnboarding::class.java
                )
                assertThat((event as WooPosCardReaderConnectionViewModel.Event.NavigateToOnboarding).onboardingState)
                    .isEqualTo(onboardingState)
            }
        }

    @Test
    fun `when startConnectionFlow called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.startConnectionFlow()

        // THEN
        verify(controller).startConnectionFlow()
    }

    @Test
    fun `when startUpdateFlow called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.startUpdateFlow()

        // THEN
        verify(controller).startUpdateFlow()
    }

    @Test
    fun `when dismissDialog called, then delegates to controller cancel`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.dismissDialog()

        // THEN
        verify(controller).cancel()
    }

    @Test
    fun `given Scanning state, when onBackPressed, then calls controller cancel`() = runTest {
        // GIVEN
        setupControllerMocks()
        controllerStateFlow.value = WooPosCardReaderConnectionState.Scanning(isRemoteTapToPaySupported = false)
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBackPressed()

        // THEN
        verify(controller).cancel()
    }

    @Test
    fun `given Connected state, when onBackPressed, then calls controller cancel`() = runTest {
        // GIVEN
        setupControllerMocks()
        controllerStateFlow.value = WooPosCardReaderConnectionState.Connected(readerName = "Reader")
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBackPressed()

        // THEN
        verify(controller).cancel()
    }

    @Test
    fun `when onBluetoothPermissionResult called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBluetoothPermissionResult(granted = true, shouldShowRationale = false)

        // THEN
        verify(controller).onBluetoothPermissionResult(granted = true, shouldShowRationale = false)
    }

    @Test
    fun `when onLocationPermissionResult called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.onLocationPermissionResult(granted = false, shouldShowRationale = true)

        // THEN
        verify(controller).onLocationPermissionResult(granted = false, shouldShowRationale = true)
    }

    @Test
    fun `when onBluetoothEnabled called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBluetoothEnabled()

        // THEN
        verify(controller).onBluetoothEnabled()
    }

    @Test
    fun `when onLocationEnabled called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.onLocationEnabled()

        // THEN
        verify(controller).onLocationEnabled()
    }

    @Test
    fun `when onOnboardingCompleted called, then delegates to controller`() = runTest {
        // GIVEN
        setupControllerMocks()
        val viewModel = createViewModel()

        // WHEN
        viewModel.onOnboardingCompleted()

        // THEN
        verify(controller).onOnboardingCompleted()
    }

    @Test
    fun `given OnboardingError state, when onBackPressed, then calls controller cancel`() = runTest {
        // GIVEN
        setupControllerMocks()
        controllerStateFlow.value = WooPosCardReaderConnectionState.OnboardingError(
            title = "Test",
            message = "Test message",
            primaryButton = null,
            onDismissClicked = {},
        )
        val viewModel = createViewModel()

        // WHEN
        viewModel.onBackPressed()

        // THEN
        verify(controller).cancel()
    }

    private fun setupControllerMocks() {
        whenever(controller.state).thenReturn(controllerStateFlow)
        whenever(controller.event).thenReturn(controllerEventFlow)
    }

    private fun createViewModel() = WooPosCardReaderConnectionViewModel(
        controllerFactory = controllerFactory,
        permissionUtils = permissionUtils,
    )
}
