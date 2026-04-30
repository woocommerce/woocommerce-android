package com.woocommerce.android.ui.woopos.settings

import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventReceiver
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventSender
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
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

    @Test
    fun `given default state, when onCategorySelected is called, then isDetailVisible stays false`() = runTest {
        // GIVEN
        whenever(childToParentEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onCategorySelected(WooPosSettingsCategory.HARDWARE)

        // THEN
        assertThat(viewModel.state.value.isDetailVisible).isFalse()
        assertThat(viewModel.state.value.canGoBack).isFalse()
        assertThat(viewModel.state.value.selectedCategory).isEqualTo(WooPosSettingsCategory.HARDWARE)
    }

    @Test
    fun `when onCategorySelectedFromPhoneList is called, then isDetailVisible is true and canGoBack is true`() = runTest {
        // GIVEN
        whenever(childToParentEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onCategorySelectedFromPhoneList(WooPosSettingsCategory.HARDWARE)

        // THEN
        assertThat(viewModel.state.value.isDetailVisible).isTrue()
        assertThat(viewModel.state.value.canGoBack).isTrue()
        assertThat(viewModel.state.value.selectedCategory).isEqualTo(WooPosSettingsCategory.HARDWARE)
    }

    @Test
    fun `given phone detail visible at root destination, when navigateBack is called, then isDetailVisible is reset`() = runTest {
        // GIVEN
        whenever(childToParentEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCategorySelectedFromPhoneList(WooPosSettingsCategory.HARDWARE)

        // WHEN
        viewModel.navigateBack()

        // THEN
        assertThat(viewModel.state.value.isDetailVisible).isFalse()
        assertThat(viewModel.state.value.canGoBack).isFalse()
    }

    @Test
    fun `given phone detail at child destination, when navigateBack is called, then walks up the destination tree`() = runTest {
        // GIVEN
        whenever(childToParentEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onCategorySelectedFromPhoneList(WooPosSettingsCategory.HARDWARE)
        viewModel.navigateToDetail(WooPosSettingsDetailDestination.Hardware.CardReaders)

        // WHEN
        viewModel.navigateBack()

        // THEN
        assertThat(viewModel.state.value.currentDestination)
            .isEqualTo(WooPosSettingsDetailDestination.Hardware.Overview)
        assertThat(viewModel.state.value.isDetailVisible).isTrue()
        assertThat(viewModel.state.value.canGoBack).isTrue()
    }

    @Test
    fun `given default state on tablet, when navigateBack is called at root, then state is unchanged`() = runTest {
        // GIVEN
        whenever(childToParentEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()
        val before = viewModel.state.value

        // WHEN
        viewModel.navigateBack()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(before)
    }

    private fun createViewModel(): WooPosSettingsViewModel {
        return WooPosSettingsViewModel(
            analyticsTracker = analyticsTracker,
            childToParentEventReceiver = childToParentEventReceiver,
            parentToChildEventSender = parentToChildEventSender,
        )
    }
}
