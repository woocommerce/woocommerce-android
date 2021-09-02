package com.woocommerce.android.ui.prefs.cardreader.update

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_SKIP_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CARD_READER_SOFTWARE_UPDATE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.InstallationStarted
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus.Unknown
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult.FAILED
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.ExplanationState
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.never
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardReaderUpdateViewModelTest : BaseUnitTest() {
    private val softwareStatus = MutableStateFlow<SoftwareUpdateStatus>(Unknown)
    private val cardReaderManager: CardReaderManager = mock {
        on { softwareUpdateStatus }.thenReturn(softwareStatus)
    }
    private val tracker: AnalyticsTrackerWrapper = mock()

    @Test
    fun `on view model init with should emit explanation state`() {
        // WHEN
        val viewModel = createViewModel()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(ExplanationState::class.java)
    }

    @Test
    fun `on view model init with skip update true should emit explanation state`() {
        // GIVEN
        val skipUpdate = true

        // WHEN
        val viewModel = createViewModel(skipUpdate)

        // THEN
        verifyExplanationState(viewModel, skipUpdate)
    }

    @Test
    fun `on view model init with skip update false should emit explanation state`() {
        // GIVEN
        val startedByUser = false

        // WHEN
        val viewModel = createViewModel(startedByUser)

        // THEN
        verifyExplanationState(viewModel, startedByUser)
    }

    @Test
    fun `when click on primary btn explanation state with unknown should emit updating state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = InstallationStarted

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingState::class.java)
        }

    @Test
    fun `when click on primary btn explanation state with installing should emit updating state`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Installing(0f)

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingState::class.java)
        }

    @Test
    fun `when click on primary btn explanation state with installation start should emit updating state with values`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = InstallationStarted

            // THEN
            verifyUpdatingState(viewModel)
        }

    @Test
    fun `when click on primary btn explanation state with success should emit exit with success result`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Success

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SUCCESS)
        }

    @Test
    fun `when click on primary btn explanation state with up to date should emit exit with skip result`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Unknown

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SKIPPED)
        }

    @Test
    fun `when click on primary btn explanation state with failed should emit exit with failed result`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Failed("")

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(FAILED)
        }

    @Test
    fun `when force update, software update status listener must be initialised on start`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            createViewModel()

            // WHEN
            softwareStatus.value = Success

            // THEN
            verify(cardReaderManager).softwareUpdateStatus
        }

    @Test
    fun `when optional update, software update status listener must not be initialised on start`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            createViewModel(startedByUser = true)

            // WHEN
            softwareStatus.value = Success

            // THEN
            verify(cardReaderManager, never()).softwareUpdateStatus
        }

    @Test
    fun `when optional update, software update status listener must be initialised on primary btn click`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel(startedByUser = true)

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Success

            // THEN
            verify(cardReaderManager).softwareUpdateStatus
        }

    @Test
    fun `when click on secondary btn explanation state should emit exit with skip result`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).secondaryButton?.onActionClicked!!.invoke()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SKIPPED)
        }

    @Test
    fun `when click on primary btn explanation state should track tap event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            verify(tracker).track(CARD_READER_SOFTWARE_UPDATE_TAPPED)
        }

    @Test
    fun `when click on primary btn explanation state with success should track success event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Success

            // THEN
            verify(tracker).track(CARD_READER_SOFTWARE_UPDATE_SUCCESS)
        }

    @Test
    fun `when click on primary btn explanation state with failed should track failed event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            verify(tracker).track(eq(CARD_READER_SOFTWARE_UPDATE_FAILED), anyOrNull(), anyOrNull(), anyOrNull())
        }

    @Test
    fun `when click on primary btn explanation state with unknown should track error event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Unknown

            // THEN
            verify(tracker).track(eq(CARD_READER_SOFTWARE_UPDATE_FAILED), anyOrNull(), anyOrNull(), anyOrNull())
        }

    @Test
    fun `when click on secondary btn explanation state should track skip event`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).secondaryButton?.onActionClicked!!.invoke()

            // THEN
            verify(tracker).track(CARD_READER_SOFTWARE_UPDATE_SKIP_TAPPED)
        }

    @Test
    fun `given user presses back, when explanation state shown, then dialog dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(ExitWithResult(FAILED))
        }

    @Test
    fun `given user presses back, when progress state shown, then dialog not dismissed`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = InstallationStarted

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat(viewModel.event.value).isNotEqualTo(ExitWithResult(FAILED))
        }

    @Test
    fun `given user presses back, when progress state shown, then cancel shown`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Installing(0f)

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingCancelingState::class.java)
        }

    @Test
    fun `given user presses back, when canceling state shown, then cancel hid`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
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
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Installing(currentProgress)

            // THEN
            assertThat((viewModel.viewStateData.value as UpdatingState).progress)
                .isEqualTo((currentProgress * 100).toInt())
        }

    @Test
    fun `given UpdatingCancelingState state shown, when update progresses, then progress percentage updated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            val currentProgress = 0.2f
            val viewModel = createViewModel()
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()
            softwareStatus.value = Installing(currentProgress)

            // WHEN
            viewModel.onBackPressed()

            // THEN
            assertThat((viewModel.viewStateData.value as UpdatingCancelingState).progress)
                .isEqualTo((currentProgress * 100).toInt())
        }

    private fun verifyExplanationState(viewModel: CardReaderUpdateViewModel, startedByUser: Boolean) {
        val state = viewModel.viewStateData.value as ExplanationState
        assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_title))
        assertThat(state.description).isEqualTo(UiStringRes(R.string.card_reader_software_update_description))
        assertThat(state.primaryButton!!.text).isEqualTo(UiStringRes(R.string.card_reader_software_update_update))
        assertThat(state.progress).isNull()
        if (startedByUser) {
            assertThat(state.secondaryButton!!.text).isEqualTo(UiStringRes(R.string.card_reader_software_update_cancel))
        } else {
            assertThat(state.secondaryButton!!.text).isEqualTo(UiStringRes(R.string.card_reader_software_update_skip))
        }
    }

    private fun verifyUpdatingState(viewModel: CardReaderUpdateViewModel) {
        val state = viewModel.viewStateData.value as UpdatingState
        assertThat(state.title).isEqualTo(UiStringRes(R.string.card_reader_software_update_in_progress_title))
        assertThat(state.description).isNull()
        assertThat(state.progress).isNotNull
        assertThat(state.progressText).isNotNull
        assertThat(state.button).isNull()
        assertThat(state.secondaryButton).isNull()
    }

    private fun createViewModel(
        startedByUser: Boolean = false
    ) = CardReaderUpdateViewModel(
        cardReaderManager,
        tracker,
        CardReaderUpdateDialogFragmentArgs(startedByUser).initSavedStateHandle()
    )
}
