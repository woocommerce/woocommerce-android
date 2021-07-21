package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.Failed
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckBluetoothEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationEnabled
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenLocationSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestEnableBluetooth
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.ShowCardReaderTutorial
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.CardReaderListItem
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.ScanningInProgressListItem
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.BluetoothDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingFailedState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.LocationDisabledError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.MissingPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.MultipleReadersFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ReaderFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningFailedState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModelTest.ScanResult.FAILED
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModelTest.ScanResult.MULTIPLE_READERS_FOUND
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModelTest.ScanResult.READER_FOUND
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModelTest.ScanResult.SCANNING
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CardReaderConnectViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CardReaderConnectViewModel

    private val tracker: AnalyticsTrackerWrapper = mock()
    private val cardReaderManager: CardReaderManager = mock()
    private val appPrefs: AppPrefs = mock()
    private val reader = mock<CardReader>().also { whenever(it.id).thenReturn("Dummy1") }
    private val reader2 = mock<CardReader>().also { whenever(it.id).thenReturn("Dummy2") }

    @Before
    fun setUp() = coroutinesTestRule.testDispatcher.runBlockingTest {
        viewModel = CardReaderConnectViewModel(
            SavedStateHandle(),
            coroutinesTestRule.testDispatchers,
            tracker,
            appPrefs,
        )
    }

    @Test
    fun `when vm initialized, then location permissions check requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            assertThat(viewModel.event.value).isInstanceOf(CheckLocationPermissions::class.java)
        }

    @Test
    fun `given permissions enabled, when connection flow started, then location enabled check emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)

            assertThat(viewModel.event.value).isInstanceOf(CheckLocationEnabled::class.java)
        }

    @Test
    fun `given permissions not enabled, when connection flow started, then permissions requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            assertThat(viewModel.event.value).isInstanceOf(RequestLocationPermissions::class.java)
        }

    @Test
    fun `given permissions granted, when permissions requested, then location enabled check emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(true)

            assertThat(viewModel.event.value).isInstanceOf(CheckLocationEnabled::class.java)
        }

    @Test
    fun `given permissions not granted, when permissions requested, then missing permissions error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(MissingPermissionsError::class.java)
        }

    @Test
    fun `when Open app settings button clicked, then user redirected to app settings`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)
            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)

            (viewModel.viewStateData.value as MissingPermissionsError).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(OpenPermissionsSettings::class.java)
        }

    @Test
    fun `given app on missing permissions error screen, when apps comes to foreground, then permissions re-checked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)
            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)

            viewModel.onScreenResumed()

            assertThat(viewModel.event.value).isInstanceOf(CheckLocationPermissions::class.java)
        }

    @Test
    fun `given NOT on missing permissions screen, when apps comes to foreground, then permissions not re-checked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)

            viewModel.onScreenResumed()

            assertThat(viewModel.event.value).isNotInstanceOf(CheckLocationPermissions::class.java)
        }

    @Test
    fun `given app on missing permissions, when apps comes to foreground, then permissions not re-requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)
            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)
            viewModel.onScreenResumed()

            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            assertThat(viewModel.event.value).isNotInstanceOf(RequestLocationPermissions::class.java)
        }

    @Test
    fun `given location disabled, when connection flow started, then location disabled error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(LocationDisabledError::class.java)
        }

    @Test
    fun `given location enabled, when connection flow started, then check bluetooth emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)

            assertThat(viewModel.event.value).isInstanceOf(CheckBluetoothEnabled::class.java)
        }

    @Test
    fun `when user clicks on open location settings, then openLocationSettings emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(false)

            (viewModel.viewStateData.value as? LocationDisabledError)?.let {
                it.onPrimaryActionClicked.invoke()
            }

            assertThat(viewModel.event.value).isInstanceOf(OpenLocationSettings::class.java)
        }

    @Test
    fun `when location settings closed, then checkLocationEnabled emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(false)
            (viewModel.viewStateData.value as? LocationDisabledError)?.let {
                it.onPrimaryActionClicked.invoke()
            }

            (viewModel.event.value as OpenLocationSettings).onLocationSettingsClosed()

            assertThat(viewModel.event.value).isInstanceOf(CheckLocationEnabled::class.java)
        }

    @Test
    fun `given bluetooth disabled, when connection flow started, then enable-bluetooth request emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
            (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(false)

            assertThat(viewModel.event.value).isInstanceOf(RequestEnableBluetooth::class.java)
        }

    @Test
    fun `given request rejected, when enable-bluetooth requested, then bluetooth disabled error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
            (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(false)

            (viewModel.event.value as RequestEnableBluetooth).onEnableBluetoothRequestResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(BluetoothDisabledError::class.java)
        }

    @Test
    fun `given request accepted, when enable-bluetooth requested, then Initialize card manager emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
            (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(false)

            (viewModel.event.value as RequestEnableBluetooth).onEnableBluetoothRequestResult(true)

            assertThat(viewModel.event.value).isInstanceOf(InitializeCardReaderManager::class.java)
        }

    @Test
    fun `when user clicks on open bluetooth settings, then enable-bluetooth request emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
            (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(false)
            (viewModel.event.value as RequestEnableBluetooth).onEnableBluetoothRequestResult(false)

            (viewModel.viewStateData.value as? BluetoothDisabledError)?.onPrimaryActionClicked?.invoke()

            assertThat(viewModel.event.value).isInstanceOf(RequestEnableBluetooth::class.java)
        }

    @Test
    fun `when cardReaderManager gets initialized, then scan is started`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init()

            verify(cardReaderManager).discoverReaders(anyBoolean())
        }

    @Test
    fun `when scan started, then scanning state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = SCANNING)

            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
        }

    @Test
    fun `when scan fails, then scanning failed state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = FAILED)

            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningFailedState::class.java)
        }

    @Test
    fun `given scanning failed screen shown, when user clicks on retry, then flow restarted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = FAILED)
            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            (viewModel.viewStateData.value as ScanningFailedState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
        }

    @Test
    fun `when reader found, then reader found state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
        }

    @Test
    fun `given last connected reader is null, when reader found, then reader found state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getLastConnectedCardReaderId()).thenReturn(null)

            init(scanState = READER_FOUND)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
        }

    @Test
    fun `given last connected reader is matching, when reader found, then reader connecting state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getLastConnectedCardReaderId()).thenReturn("Dummy1")

            init(scanState = READER_FOUND)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectingState::class.java)
        }

    @Test
    fun `given last connected reader is matching, when reader found, then auto reconnection event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getLastConnectedCardReaderId()).thenReturn("Dummy1")

            init(scanState = READER_FOUND)

            verify(tracker).track(AnalyticsTracker.Stat.CARD_READER_AUTO_CONNECTION_STARTED)
        }

    @Test
    fun `given last connected reader is not matching, when reader found, then reader found state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getLastConnectedCardReaderId()).thenReturn("Dummy2")

            init(scanState = READER_FOUND)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
        }

    @Test
    fun `given reader id is null, when reader found, then reader is ignored`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reader.id).thenReturn(null)

            init(scanState = READER_FOUND)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
        }

    @Test
    fun `when multiple readers found, then multiple readers found state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = MULTIPLE_READERS_FOUND)

            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(MultipleReadersFoundState::class.java)
        }

    @Test
    fun `when scanning fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = FAILED)

            verify(tracker).track(
                eq(AnalyticsTracker.Stat.CARD_READER_DISCOVERY_FAILED), anyOrNull(), anyOrNull(), anyOrNull()
            )
        }

    @Test
    fun `when reader found, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            verify(tracker)
                .track(AnalyticsTracker.Stat.CARD_READER_DISCOVERY_READER_DISCOVERED, mapOf("reader_count" to 1))
        }

    @Test
    fun `when multiple readers found, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = MULTIPLE_READERS_FOUND)

            verify(tracker)
                .track(AnalyticsTracker.Stat.CARD_READER_DISCOVERY_READER_DISCOVERED, mapOf("reader_count" to 2))
        }

    @Test
    fun `when user clicks on connect to reader button, then app starts connecting to reader`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init()

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(cardReaderManager).connectToReader(reader)
        }

    @Test
    fun `given multiple readers found, when user clicks on connect, then app connects to the correct reader`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = MULTIPLE_READERS_FOUND)

            val reader = (viewModel.viewStateData.value as MultipleReadersFoundState).listItems[1] as CardReaderListItem
            reader.onConnectClicked()

            verify(cardReaderManager).connectToReader(argThat { this.id == reader.readerId })
        }

    @Test
    fun `when multiple readers found, then scanning in progress item shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = MULTIPLE_READERS_FOUND)

            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat((viewModel.viewStateData.value as MultipleReadersFoundState).listItems.last())
                .isInstanceOf(ScanningInProgressListItem::class.java)
        }

    @Test
    fun `given user clicks on connect, when reader found, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(tracker)
                .track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_TAPPED)
        }

    @Test
    fun `given user clicks on connect, when multiple readers found, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = MULTIPLE_READERS_FOUND)

            val reader = (viewModel.viewStateData.value as MultipleReadersFoundState).listItems[1] as CardReaderListItem
            reader.onConnectClicked()

            verify(tracker)
                .track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_TAPPED)
        }

    @Test
    fun `when app is connecting to reader, then connecting state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init()

            pauseDispatcher()
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectingState::class.java)
            resumeDispatcher()
        }

    @Test
    fun `when app successfully connects to reader, then connection flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = true)

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(true))
        }

    @Test
    fun `when app successfully connects to reader, then reader id stored`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = true)

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(appPrefs).setLastConnectedCardReaderId("Dummy1")
        }

    @Test
    fun `when connecting to reader succeeds, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = true)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(tracker).track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_SUCCESS)
        }

    @Test
    fun `when connecting to reader for the first time, then the tutorial shows`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getShowCardReaderConnectedTutorial()).thenReturn(true)
            init(connectingSucceeds = true)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()
            assertThat(viewModel.event.value).isInstanceOf(ShowCardReaderTutorial::class.java)
        }

    @Test
    fun `when connecting to reader not for the first time, then the tutorial doesn't show`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(appPrefs.getShowCardReaderConnectedTutorial()).thenReturn(false)
            init(connectingSucceeds = true)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()
            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(true))
        }

    @Test
    fun `when connecting to reader fails, then connecting failed state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = false)

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectingFailedState::class.java)
        }

    @Test
    fun `when connecting to reader fails, then event tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = false)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(tracker).track(AnalyticsTracker.Stat.CARD_READER_CONNECTION_FAILED)
        }

    @Test
    fun `given connecting failed screen shown, when user clicks on retry, then flow restarted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = false)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            pauseDispatcher()
            (viewModel.viewStateData.value as ConnectingFailedState).onPrimaryActionClicked()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
            resumeDispatcher()
        }

    @Test
    fun `given app in scanning state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = SCANNING)

            (viewModel.viewStateData.value as ScanningState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(false))
        }

    @Test
    fun `given app in reader found state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            (viewModel.viewStateData.value as ReaderFoundState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(false))
        }

    @Test
    fun `given app in connecting state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            pauseDispatcher()
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()
            (viewModel.viewStateData.value as ConnectingState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(false))
            resumeDispatcher()
        }

    @Test
    fun `given app in scanning failed state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = FAILED)
            (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            (viewModel.viewStateData.value as ScanningFailedState).onSecondaryActionClicked.invoke()
        }

    @Test
    fun `given app in connecting failed state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(connectingSucceeds = false)
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            (viewModel.viewStateData.value as ConnectingFailedState).onSecondaryActionClicked()

            assertThat(viewModel.event.value).isEqualTo(Event.ExitWithResult(false))
        }

    @Test
    fun `when app in scanning state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = SCANNING)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_scanning_header))
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isEqualTo(R.string.card_reader_connect_scanning_hint)
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_card_reader_scanning)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_200)
        }

    @Test
    fun `when app in readers found state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(
                    UiStringRes(
                        R.string.card_reader_connect_reader_found_header,
                        listOf(UiStringText("<b>${reader.id}</b>")),
                        true
                    )
                )
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.card_reader_connect_to_reader)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_card_reader)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_275)
        }

    @Test
    fun `when app in connecting state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND)

            viewModel.viewStateData.value!!.onPrimaryActionClicked!!.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectingState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_connecting_header))
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isEqualTo(R.string.card_reader_connect_connecting_hint)
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_card_reader_connecting)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_275)
        }

    @Test
    fun `when app in scanning failed state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = FAILED)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningFailedState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_scanning_failed_header))
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.try_again)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_products_error)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_150)
        }

    @Test
    fun `when app in connecting failed state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(scanState = READER_FOUND, connectingSucceeds = false)

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            assertThat(viewModel.viewStateData.value).isInstanceOf(ConnectingFailedState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_failed_header))
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.try_again)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_products_error)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_150)
        }

    @Test
    fun `when app in missing location permissions state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)
            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(MissingPermissionsError::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_missing_permissions_header))
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.card_reader_connect_open_permission_settings)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_products_error)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_150)
        }

    @Test
    fun `when app in location disabled state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(LocationDisabledError::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_location_provider_disabled_header))
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.card_reader_connect_open_location_settings)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_products_error)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_150)
        }

    @Test
    fun `when app in bluetooth disabled state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
            (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
            (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(false)
            (viewModel.event.value as RequestEnableBluetooth).onEnableBluetoothRequestResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(BluetoothDisabledError::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(UiStringRes(R.string.card_reader_connect_bluetooth_disabled_header))
            assertThat(viewModel.viewStateData.value!!.hintLabel)
                .describedAs("Check hint")
                .isNull()
            assertThat(viewModel.viewStateData.value!!.primaryActionLabel)
                .describedAs("Check primaryActionLabel")
                .isEqualTo(R.string.card_reader_connect_open_bluetooth_settings)
            assertThat(viewModel.viewStateData.value!!.secondaryActionLabel)
                .describedAs("Check secondaryActionLabel")
                .isEqualTo(R.string.cancel)
            assertThat(viewModel.viewStateData.value!!.illustration)
                .describedAs("Check illustration")
                .isEqualTo(R.drawable.img_products_error)
            assertThat(viewModel.viewStateData.value!!.illustrationTopMargin)
                .describedAs("Check illustration vertical margin")
                .isEqualTo(R.dimen.major_150)
        }

    private suspend fun init(scanState: ScanResult = READER_FOUND, connectingSucceeds: Boolean = true) {
        whenever(cardReaderManager.discoverReaders(anyBoolean())).thenAnswer {
            flow {
                when (scanState) {
                    SCANNING -> { // no-op
                    }
                    READER_FOUND -> emit(ReadersFound(listOf(reader)))
                    MULTIPLE_READERS_FOUND -> emit(ReadersFound(listOf(reader, reader2)))
                    FAILED -> emit(Failed("dummy msg"))
                }
            }
        }
        whenever(cardReaderManager.connectToReader(reader)).thenReturn(connectingSucceeds)
        whenever(cardReaderManager.connectToReader(reader2)).thenReturn(connectingSucceeds)
        (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
        (viewModel.event.value as CheckLocationEnabled).onLocationEnabledCheckResult(true)
        (viewModel.event.value as CheckBluetoothEnabled).onBluetoothCheckResult(true)
        (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)
    }

    private enum class ScanResult {
        SCANNING, READER_FOUND, MULTIPLE_READERS_FOUND, FAILED
    }
}
