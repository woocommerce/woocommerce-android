package com.woocommerce.android.ui.prefs.cardreader.update

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Failed
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Initializing
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Installing
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.Success
import com.woocommerce.android.cardreader.SoftwareUpdateStatus.UpToDate
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.UpdateResult
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.ExplanationState
import com.woocommerce.android.ui.prefs.cardreader.update.CardReaderUpdateViewModel.ViewState.UpdatingState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class CardReaderUpdateViewModelTest : BaseUnitTest() {
    private val cardReaderManager: CardReaderManager = mock()

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
        val skipUpdate = false

        // WHEN
        val viewModel = createViewModel(skipUpdate)

        // THEN
        verifyExplanationState(viewModel, skipUpdate)
    }

    @Test
    fun `when click on primary btn explanation state with initializing should emit updating state`() = runBlockingTest {
        // GIVEN
        val viewModel = createViewModel()
        whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(Initializing))

        // WHEN
        (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingState::class.java)
    }

    @Test
    fun `when click on primary btn explanation state with installing should emit updating state`() = runBlockingTest {
        // GIVEN
        val viewModel = createViewModel()
        whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(Installing(0f)))

        // WHEN
        (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

        // THEN
        assertThat(viewModel.viewStateData.value).isInstanceOf(UpdatingState::class.java)
    }

    @Test
    fun `when click on primary btn explanation state with initializing should emit updating state with values`() =
        runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(Initializing))

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            verifyUpdatingState(viewModel)
        }

    @Test
    fun `when click on primary btn explanation state with success should emit exit with success result`() =
        runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(Success))

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SUCCESS)
        }

    @Test
    fun `when click on primary btn explanation state with up to date should emit exit with skip result`() =
        runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(UpToDate))

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SKIPPED)
        }

    @Test
    fun `when click on primary btn explanation state with failed should emit exit with failed result`() =
        runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()
            whenever(cardReaderManager.updateSoftware()).thenReturn(MutableStateFlow(Failed("")))

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).primaryButton?.onActionClicked!!.invoke()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.FAILED)
        }

    @Test
    fun `when click on secondary btn explanation state should emit exit with skip result`() =
        runBlockingTest {
            // GIVEN
            val viewModel = createViewModel()

            // WHEN
            (viewModel.viewStateData.value as ExplanationState).secondaryButton?.onActionClicked!!.invoke()

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ExitWithResult::class.java)
            assertThat((viewModel.event.value as ExitWithResult<*>).data).isEqualTo(UpdateResult.SKIPPED)
        }

    private fun verifyExplanationState(viewModel: CardReaderUpdateViewModel, skipUpdate: Boolean) {
        val state = viewModel.viewStateData.value as ExplanationState
        assertThat(state.title).isEqualTo(UiStringRes(string.card_reader_software_update_title))
        assertThat(state.description).isEqualTo(UiStringRes(string.card_reader_software_update_description))
        assertThat(state.primaryButton!!.text).isEqualTo(UiStringRes(string.card_reader_software_update_update))
        assertThat(state.showProgress).isFalse()
        if (skipUpdate) {
            assertThat(state.secondaryButton!!.text).isEqualTo(UiStringRes(string.card_reader_software_update_skip))
        } else {
            assertThat(state.secondaryButton!!.text).isEqualTo(UiStringRes(string.card_reader_software_update_cancel))
        }
    }

    private fun verifyUpdatingState(viewModel: CardReaderUpdateViewModel) {
        val state = viewModel.viewStateData.value as UpdatingState
        assertThat(state.title).isEqualTo(UiStringRes(string.card_reader_software_update_in_progress_title))
        assertThat(state.description).isNull()
        assertThat(state.showProgress).isTrue()
        assertThat(state.primaryButton).isNull()
        assertThat(state.secondaryButton).isNull()
    }

    private fun createViewModel(
        skipUpdate: Boolean = false
    ) = CardReaderUpdateViewModel(
        cardReaderManager,
        CardReaderUpdateDialogFragmentArgs(skipUpdate).initSavedStateHandle()
    )
}
