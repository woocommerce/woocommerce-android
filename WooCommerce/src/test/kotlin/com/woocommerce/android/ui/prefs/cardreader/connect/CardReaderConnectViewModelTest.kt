package com.woocommerce.android.ui.prefs.cardreader.connect

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReader
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents.ReadersFound
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.CheckLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.InitializeCardReaderManager
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.OpenPermissionsSettings
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.CardReaderConnectEvent.RequestLocationPermissions
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ConnectingState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.MissingPermissionsError
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ReaderFoundState
import com.woocommerce.android.ui.prefs.cardreader.connect.CardReaderConnectViewModel.ViewState.ScanningState
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

    private val cardReaderManager: CardReaderManager = mock()
    private val reader = mock<CardReader>().also { whenever(it.getId()).thenReturn("dummy id") }

    @Before
    fun setUp() = runBlockingTest {
        viewModel = CardReaderConnectViewModel(
            SavedStateHandle(),
            coroutinesTestRule.testDispatchers,
            mock()
        )
    }

    @Test
    fun `when vm initilized, then location permissions check requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            assertThat(viewModel.event.value).isInstanceOf(CheckLocationPermissions::class.java)
        }

    @Test
    fun `given permissions enabled, when connection flow started, then initialize cardReaderManager request emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)

            assertThat(viewModel.event.value).isInstanceOf(InitializeCardReaderManager::class.java)
        }

    @Test
    fun `given permissions not enabled, when connection flow started, then permissions requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            assertThat(viewModel.event.value).isInstanceOf(RequestLocationPermissions::class.java)
        }

    @Test
    fun `given permissions granted, when permissions requested, then initialize cardReaderManager request emitted`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(true)

            assertThat(viewModel.event.value).isInstanceOf(InitializeCardReaderManager::class.java)
        }

    @Test
    fun `given permissions not granted, when permissions requested, then missing permissions error shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)

            assertThat(viewModel.viewStateData.value).isInstanceOf(MissingPermissionsError::class.java)
        }

    @Test
    fun `when Open app settings button clicked, then user redirect to app settings`() =
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
    fun `given app on missing permissions, when apps comes to foreground, then permissions not requested`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)
            (viewModel.event.value as RequestLocationPermissions).onPermissionsRequestResult(false)
            viewModel.onScreenResumed()

            (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(false)

            assertThat(viewModel.event.value).isNotInstanceOf(RequestLocationPermissions::class.java)
        }

    @Test
    fun `when cardReaderManager gets initilized, then scan is started`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init()

            verify(cardReaderManager).discoverReaders(anyBoolean())
        }

    @Test
    fun `when scan started, then scanning state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = false)

        (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
        }

    @Test
    fun `when reader found, then reader found state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = true)

        (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
        }

    @Test
    fun `given reader id is null, when reader found, then reader is ignored`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            whenever(reader.getId()).thenReturn(null)

            init(readersFound = true)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ScanningState::class.java)
        }

    @Test
    fun `when user clicks on connect to reader button, then app starts connecting to reader`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init()

            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()

            verify(cardReaderManager).connectToReader(reader)
        }

    @Test
    fun `when app is conneting to reader, then connecting state shown`() =
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

            assertThat(viewModel.event.value).isInstanceOf(Event.Exit::class.java)
        }

    @Test
    fun `given app in scanning state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = false)

            (viewModel.viewStateData.value as ScanningState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Event.Exit::class.java)
        }

    @Test
    fun `given app in reader found state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = true)

            (viewModel.viewStateData.value as ReaderFoundState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Event.Exit::class.java)
        }

    @Test
    fun `given app in connecting state, when user clicks on cancel, then flow finishes`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = true)

            pauseDispatcher()
            (viewModel.viewStateData.value as ReaderFoundState).onPrimaryActionClicked.invoke()
            (viewModel.viewStateData.value as ConnectingState).onSecondaryActionClicked.invoke()

            assertThat(viewModel.event.value).isInstanceOf(Event.Exit::class.java)
            resumeDispatcher()
        }

    @Test
    fun `when app in scanning state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = false)

            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(R.string.card_reader_connect_scanning_header)
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
        }

    @Test
    fun `when app in readers found state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = true)

            assertThat(viewModel.viewStateData.value).isInstanceOf(ReaderFoundState::class.java)
            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(R.string.card_reader_connect_reader_found_header)
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
        }

    @Test
    fun `when app in connecting state, then correct labels and illustrations shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            init(readersFound = true)

            pauseDispatcher()
            viewModel.viewStateData.value!!.onPrimaryActionClicked!!.invoke()

            assertThat(viewModel.viewStateData.value!!.headerLabel)
                .describedAs("Check header")
                .isEqualTo(R.string.card_reader_connect_connecting_header)
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
        }

    private suspend fun init(readersFound: Boolean = true, connectingSucceeds: Boolean = true) {
        whenever(cardReaderManager.discoverReaders(anyBoolean())).thenAnswer {
            flow<CardReaderDiscoveryEvents> {
                if (readersFound) {
                    emit(ReadersFound(listOf(reader)))
                }
            }
        }
        whenever(cardReaderManager.connectToReader(reader)).thenReturn(connectingSucceeds)
        (viewModel.event.value as CheckLocationPermissions).onPermissionsCheckResult(true)
        (viewModel.event.value as InitializeCardReaderManager).onCardManagerInitialized(cardReaderManager)
    }
}
