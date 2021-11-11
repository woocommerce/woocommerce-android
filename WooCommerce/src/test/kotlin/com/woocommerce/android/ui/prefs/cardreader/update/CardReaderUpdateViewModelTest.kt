package com.woocommerce.android.ui.prefs.cardreader.update

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.InstallationStarted
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Unknown
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.CardReaderUpdateEvent.SoftwareUpdateAboutToStart
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.CardReaderUpdateEvent.SoftwareUpdateProgress
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.SUCCESS
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdateAboutToStart
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdateFailedBatteryLow
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingCancelingState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardReaderUpdateViewModelTest : BaseUnitTest() {
    private val softwareStatus = MutableStateFlow<SoftwareUpdateStatus>(Unknown)
    private val cardReaderManager: CardReaderManager = mock {
        on { softwareUpdateStatus }.thenReturn(softwareStatus)
    }
    private val tracker: AnalyticsTrackerWrapper = mock()
    companion object {
        private const val REQUIRED_UPDATE = "Required"
        private const val OPTIONAL_UPDATE = "Optional"
    }

    @Test
    fun `given required update, when view model created, then installation not started`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val requiredUpdate = true

            // WHEN
            createViewModel(requiredUpdate = requiredUpdate)

            // THEN
            verify(cardReaderManager, never()).startAsyncSoftwareUpdate()
        }

    @Test
    fun `given optional update, when view model created, then installation started`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val requiredUpdate = false

            // WHEN
            createViewModel(requiredUpdate = requiredUpdate)

            // THEN
            verify(cardReaderManager).startAsyncSoftwareUpdate()
        }

    @Test
    fun `given installation started status, when view model created, then update about to start`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = InstallationStarted

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            val state = viewModel.viewStateData.value as UpdateAboutToStart
            assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_in_progress_title))
            assertThat(state.description).isEqualTo(UiStringRes(R.string.card_reader_software_update_description))
            assertThat(state.progressText).isEqualTo(buildProgressText(0))
            assertThat(state.progress).isEqualTo(0)
            assertThat(state.illustration).isEqualTo(R.drawable.img_card_reader_update_progress)
            assertThat(state.button?.text).isEqualTo(null)
        }

    @Test
    fun `given installation about to start, when view model created, then announce for accessibility`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = InstallationStarted

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status
            viewModel.viewStateData.value as UpdateAboutToStart

            // THEN
            assertThat(viewModel.event.value).isEqualTo(
                SoftwareUpdateAboutToStart(
                    R.string.card_reader_software_update_description
                )
            )
        }

    @Test
    fun `given installation 10 status, when view model created, then announce update progress for accessibility`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Installing(.1f)

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            val uiString = UiStringRes(
                R.string.card_reader_software_update_progress_indicator,
                params = listOf(
                    UiString.UiStringText(text = "10", containsHtml = false)
                ),
                containsHtml = false
            )
            assertThat(viewModel.event.value).isEqualTo(SoftwareUpdateProgress(uiString))
        }

    @Test
    fun `given installing 10 status, when view model created, then updating state with 10`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Installing(.1f)

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            verifyUpdatingState(viewModel, 10)
        }

    @Test
    fun `given failed status with failed type, when view model created, then exit with failed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val reason = "reason"
            val status = Failed(SoftwareUpdateStatusErrorType.Failed, reason)

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(FAILED)
        }

    @Test
    fun `given failed status with battery low type, when view model created, then battery low status`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val reason = "reason"
            val batteryLevel = 0.3f
            val status = Failed(SoftwareUpdateStatusErrorType.BatteryLow(batteryLevel), reason)

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            val state = viewModel.viewStateData.value as UpdateFailedBatteryLow
            assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_title_battery_low))
            assertThat(state.description).isEqualTo(
                UiStringRes(
                    R.string.card_reader_software_update_progress_description_low_battery,
                    listOf(UiString.UiStringText("30"))
                )
            )
            assertThat(state.progressText).isNull()
            assertThat(state.progress).isNull()
            assertThat(state.illustration).isEqualTo(R.drawable.img_card_reader_update_failed_battery_low)
        }

    @Test
    fun `given failed status with battery low with battery level, when view model created, then special description`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val reason = "reason"
            val status = Failed(SoftwareUpdateStatusErrorType.BatteryLow(null), reason)

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            val state = viewModel.viewStateData.value as UpdateFailedBatteryLow
            assertThat(state.description).isEqualTo(
                UiStringRes(
                    R.string.card_reader_software_update_progress_description_low_battery_level_unknown
                )
            )
        }

    @Test
    fun `given success status, when view model created, then exit with success`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Success

            // WHEN
            val viewModel = createViewModel()
            softwareStatus.value = status

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(SUCCESS)
        }

    @Test
    fun `given success status for required update, when view model created, then success tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Success

            // WHEN
            createViewModel(requiredUpdate = true)
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_SUCCESS,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE
                )
            )
        }

    @Test
    fun `given success status for optional update, when view model created, then success tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Success

            // WHEN
            createViewModel()
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_SUCCESS,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE
                )
            )
        }

    @Test
    fun `given failed status for optional update, when view model created, then failed tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Failed(SoftwareUpdateStatusErrorType.Failed, "Failed")

            // WHEN
            createViewModel()
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderUpdateViewModel",
                    AnalyticsTracker.KEY_ERROR_DESC to status.message
                )
            )
        }

    @Test
    fun `given required update started, when view model created, then started tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = InstallationStarted

            // WHEN
            createViewModel(requiredUpdate = true)
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_STARTED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE
                )
            )
        }

    @Test
    fun `given optional update started, when view model created, then started tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = InstallationStarted

            // WHEN
            createViewModel()
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_STARTED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE
                )
            )
        }

    @Test
    fun `given failed status for required update, when view model created, then failed tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val status = Failed(SoftwareUpdateStatusErrorType.Failed, "")

            // WHEN
            createViewModel(requiredUpdate = true)
            softwareStatus.value = status

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderUpdateViewModel",
                    AnalyticsTracker.KEY_ERROR_DESC to status.message
                )
            )
        }

    @Test
    fun `given user presses cancel, when optional update is shown, then failed event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            softwareStatus.value = Installing(0.5f)
            val viewModel = createViewModel()

            // WHEN
            viewModel.onBackPressed()
            (viewModel.viewStateData.value as UpdatingCancelingState).button.onActionClicked.invoke()

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to OPTIONAL_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderUpdateViewModel",
                    AnalyticsTracker.KEY_ERROR_DESC to "User manually cancelled the flow"
                )
            )
        }

    @Test
    fun `given user presses cancel, when required update is shown, then failed event is tracked`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            softwareStatus.value = Installing(0.5f)
            val viewModel = createViewModel(requiredUpdate = true)

            // WHEN
            viewModel.onBackPressed()
            (viewModel.viewStateData.value as UpdatingCancelingState).button.onActionClicked.invoke()

            // THEN
            verify(tracker).track(
                CARD_READER_SOFTWARE_UPDATE_FAILED,
                hashMapOf(
                    AnalyticsTracker.KEY_SOFTWARE_UPDATE_TYPE to REQUIRED_UPDATE,
                    AnalyticsTracker.KEY_ERROR_CONTEXT to "CardReaderUpdateViewModel",
                    AnalyticsTracker.KEY_ERROR_DESC to "User manually cancelled the flow"
                )
            )
        }

    @Test
    fun `given user presses back, when progress state shown, then dialog not dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            softwareStatus.value = InstallationStarted
            val viewModel = createViewModel()

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat(viewModel.event.value).isNotEqualTo(ExitWithResult(FAILED))
        }

    @Test
    fun `given required update user presses back, when progress state shown, then updating canceling state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val requiredUpdate = true
            val viewModel = createViewModel(requiredUpdate = requiredUpdate)
            softwareStatus.value = Installing(.1f)

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verifyUpdatingCancelingState(viewModel, requiredUpdate, 10)
        }

    @Test
    fun `given optional update user presses back, when progress state shown, then updating canceling state shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val requiredUpdate = false
            val viewModel = createViewModel(requiredUpdate = requiredUpdate)
            softwareStatus.value = Installing(.1f)

            // WHEN
            viewModel.onBackPressed()

            // THEN
            verifyUpdatingCancelingState(viewModel, requiredUpdate, 10)
        }

    @Test
    fun `given user presses back, when canceling state shown, then cancel hid`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            softwareStatus.value = Installing(0f)
            viewModel.onBackPressed()

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingState::class.java)
        }

    @Test
    fun `given UpdatingState state shown, when update progresses, then progress percentage updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val currentProgress = 0.2f
            val viewModel = createViewModel()

            // WHEN
            softwareStatus.value = Installing(currentProgress)

            // THEN
            val updatingState = viewModel.viewStateData.value as UpdatingState
            assertThat(updatingState.progressText).isEqualTo(buildProgressText((currentProgress * 100).toInt()))
            assertThat(updatingState.progress).isEqualTo(20)
        }

    @Test
    fun `given UpdatingCancelingState state shown, when update progresses, then progress percentage updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val currentProgress = 0.2f
            val viewModel = createViewModel()
            softwareStatus.value = Installing(currentProgress)

            // WHEN
            viewModel.onBackPressed()

            // THEN
            val updatingCancelingState = viewModel.viewStateData.value as UpdatingCancelingState
            assertThat(updatingCancelingState.progressText)
                .isEqualTo(buildProgressText((currentProgress * 100).toInt()))
            assertThat(updatingCancelingState.progress).isEqualTo(20)
        }

    private fun verifyUpdatingState(viewModel: CardReaderUpdateViewModel, progress: Int) {
        val state = viewModel.viewStateData.value as UpdatingState
        assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_in_progress_title))
        assertThat(state.description).isEqualTo(UiStringRes(R.string.card_reader_software_update_description))
        assertThat(state.progressText).isEqualTo(buildProgressText(progress))
        assertThat(state.progress).isEqualTo(progress)
        assertThat(state.illustration).isEqualTo(R.drawable.img_card_reader_update_progress)
    }

    private fun verifyUpdatingCancelingState(
        viewModel: CardReaderUpdateViewModel,
        requiredUpdate: Boolean,
        progress: Int
    ) {
        val state = viewModel.viewStateData.value as UpdatingCancelingState
        assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_in_progress_title))
        if (requiredUpdate) {
            assertThat(state.description).isEqualTo(
                UiStringRes(R.string.card_reader_software_update_progress_cancel_required_warning)
            )
        } else {
            assertThat(state.description).isEqualTo(
                UiStringRes(R.string.card_reader_software_update_progress_cancel_warning)
            )
        }
        assertThat(state.illustration).isEqualTo(R.drawable.img_card_reader_update_progress)
        assertThat(state.progressText).isEqualTo(buildProgressText(progress))
        assertThat(state.button.text).isEqualTo(UiStringRes(R.string.cancel_anyway))
    }

    private fun buildProgressText(progress: Int) =
        UiStringRes(
            R.string.card_reader_software_update_progress_indicator,
            listOf(UiString.UiStringText(progress.toString()))
        )

    private fun createViewModel(
        requiredUpdate: Boolean = false
    ) = CardReaderUpdateViewModel(
        cardReaderManager,
        tracker,
        CardReaderUpdateDialogFragmentArgs(requiredUpdate).initSavedStateHandle()
    )
}
