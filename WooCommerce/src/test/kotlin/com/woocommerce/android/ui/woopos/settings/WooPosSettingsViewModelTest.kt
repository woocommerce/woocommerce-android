package com.woocommerce.android.ui.woopos.settings

import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventReceiver
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosSettingsViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val childToParentEventReceiver: WooPosChildrenToParentEventReceiver = mock()
    private val parentToChildEventSender: WooPosParentToChildrenEventSender = mock()

    @Test
    fun `when ShowSyncErrorDialog event collected, then dialog state is shown`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(childToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog(errorMessage))
        )

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.dialogState)
            .isInstanceOf(WooPosSettingsDialogState.SyncErrorDialog::class.java)
        assertThat((viewModel.state.value.dialogState as WooPosSettingsDialogState.SyncErrorDialog).errorMessage)
            .isEqualTo(errorMessage)
    }

    @Test
    fun `given sync error dialog shown, when hideDialog called, then dialog is hidden`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(childToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog(errorMessage))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.hideDialog()

        // THEN
        assertThat(viewModel.state.value.dialogState).isEqualTo(WooPosSettingsDialogState.Hidden)
    }

    @Test
    fun `given sync error dialog shown, when retrySyncFromDialog called, then dialog is hidden`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(childToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog(errorMessage))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onRetrySyncFromDialogClicked()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value.dialogState).isEqualTo(WooPosSettingsDialogState.Hidden)
    }

    @Test
    fun `given sync error dialog shown, when retrySyncFromDialog called, then RetrySyncRequested event is sent`() = runTest {
        // GIVEN
        val errorMessage = "Network error"
        whenever(childToParentEventReceiver.events).thenReturn(
            flowOf(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog(errorMessage))
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onRetrySyncFromDialogClicked()
        advanceUntilIdle()

        // THEN
        verify(parentToChildEventSender).sendToChildren(
            argThat {
                this is ParentToChildrenEvent.SettingsEvent.RetrySyncRequested
            }
        )
    }

    @Test
    fun `when default state is created, then dialog state is Hidden`() {
        // GIVEN & WHEN
        val initialState = WooPosSettingsState()

        // THEN
        assertThat(initialState.dialogState).isEqualTo(WooPosSettingsDialogState.Hidden)
    }

    @Test
    fun `given multiple error events, when received, then dialog state is updated for each`() = runTest {
        // GIVEN
        val eventsFlow = MutableSharedFlow<ChildToParentEvent>()
        whenever(childToParentEventReceiver.events).thenReturn(eventsFlow)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN & THEN
        eventsFlow.emit(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog("Error 1"))
        advanceUntilIdle()
        assertThat(viewModel.state.value.dialogState)
            .isInstanceOf(WooPosSettingsDialogState.SyncErrorDialog::class.java)
        assertThat((viewModel.state.value.dialogState as WooPosSettingsDialogState.SyncErrorDialog).errorMessage)
            .isEqualTo("Error 1")

        viewModel.hideDialog()

        eventsFlow.emit(ChildToParentEvent.SettingsEvent.ShowSyncErrorDialog("Error 2"))
        advanceUntilIdle()
        assertThat((viewModel.state.value.dialogState as WooPosSettingsDialogState.SyncErrorDialog).errorMessage)
            .isEqualTo("Error 2")
    }

    private fun createViewModel(): WooPosSettingsViewModel {
        return WooPosSettingsViewModel(
            analyticsTracker = analyticsTracker,
            childToParentEventReceiver = childToParentEventReceiver,
            parentToChildEventSender = parentToChildEventSender,
        )
    }
}
