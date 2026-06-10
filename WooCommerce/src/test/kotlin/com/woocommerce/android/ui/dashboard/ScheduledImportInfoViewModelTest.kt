package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.ui.dashboard.data.AnalyticsScheduledImportRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class ScheduledImportInfoViewModelTest : BaseUnitTest() {
    private val scheduledImportRepository: AnalyticsScheduledImportRepository = mock()

    private lateinit var viewModel: ScheduledImportInfoViewModel

    private fun createViewModel(isEnabled: Boolean = false) {
        viewModel = ScheduledImportInfoViewModel(
            savedState = SavedStateHandle(mapOf("isEnabled" to isEnabled)),
            scheduledImportRepository = scheduledImportRepository,
        )
    }

    @Test
    fun `given enabled nav arg, when the sheet is shown, then initial state reflects it`() = testBlocking {
        createViewModel(isEnabled = true)

        assertThat(viewModel.viewState.getOrAwaitValue().isEnabled).isTrue()
    }

    @Test
    fun `given disabled nav arg, when the sheet is shown, then initial state reflects it`() = testBlocking {
        createViewModel(isEnabled = false)

        assertThat(viewModel.viewState.getOrAwaitValue().isEnabled).isFalse()
    }

    @Test
    fun `when toggle is changed and update succeeds, then state reflects the new value and event is triggered`() =
        testBlocking {
            createViewModel(isEnabled = false)
            whenever(scheduledImportRepository.setEnabled(true)).thenReturn(WooResult(true))

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onOptionSelected(true)
            }.last()

            val state = viewModel.viewState.getOrAwaitValue()
            assertThat(state.isEnabled).isTrue()
            assertThat(state.isUpdating).isFalse()
            assertThat(state.hasError).isFalse()
            assertThat(event).isEqualTo(ScheduledImportInfoViewModel.SettingUpdated)
        }

    @Test
    fun `when toggle is changed and update fails, then state reverts and shows an error`() = testBlocking {
        createViewModel(isEnabled = false)
        whenever(scheduledImportRepository.setEnabled(true)).thenReturn(
            WooResult(
                error = WooError(
                    type = WooErrorType.GENERIC_ERROR,
                    original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                    message = "error"
                )
            )
        )

        viewModel.onOptionSelected(true)

        val state = viewModel.viewState.getOrAwaitValue()
        assertThat(state.isEnabled).isFalse()
        assertThat(state.isUpdating).isFalse()
        assertThat(state.hasError).isTrue()
    }

    @Test
    fun `given an update is in progress, when toggle is changed again, then the second change is ignored`() =
        testBlocking {
            createViewModel(isEnabled = false)
            val gate = CompletableDeferred<WooResult<Boolean>>()
            whenever(scheduledImportRepository.setEnabled(true)).doSuspendableAnswer { gate.await() }

            // First toggle suspends on the gate, keeping isUpdating = true
            viewModel.onOptionSelected(true)
            assertThat(viewModel.viewState.getOrAwaitValue().isUpdating).isTrue()

            // Second toggle while the first is still in flight must be ignored
            viewModel.onOptionSelected(false)
            gate.complete(WooResult(true))

            verify(scheduledImportRepository).setEnabled(true)
            verify(scheduledImportRepository, never()).setEnabled(false)
        }

    @Test
    fun `when learn more is clicked, then docs url is launched`() = testBlocking {
        createViewModel()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onLearnMoreClicked()
        }.last()

        assertThat(event).isEqualTo(LaunchUrlInChromeTab(AppUrls.ANALYTICS_SCHEDULED_IMPORT_DOCS))
    }

    @Test
    fun `given update fails, when toggle is changed, then no setting updated event is triggered`() = testBlocking {
        createViewModel(isEnabled = false)
        whenever(scheduledImportRepository.setEnabled(true)).thenReturn(
            WooResult(
                error = WooError(
                    type = WooErrorType.GENERIC_ERROR,
                    original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                    message = "error"
                )
            )
        )

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onOptionSelected(true)
        }

        assertThat(events.filterIsInstance<ScheduledImportInfoViewModel.SettingUpdated>()).isEmpty()
    }

    @Test
    fun `given an option is already selected, when it is tapped, then the sheet closes without calling the repository`() =
        testBlocking {
            createViewModel(isEnabled = true)

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onOptionSelected(true)
            }.last()

            assertThat(event).isEqualTo(ScheduledImportInfoViewModel.SettingUpdated)
            verify(scheduledImportRepository, never()).setEnabled(true)
        }
}
