package com.woocommerce.android.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.ui.dashboard.data.AnalyticsScheduledImportRepository
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
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
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@ExperimentalCoroutinesApi
class ScheduledImportInfoViewModelTest : BaseUnitTest() {
    private val scheduledImportRepository: AnalyticsScheduledImportRepository = mock()

    private lateinit var viewModel: ScheduledImportInfoViewModel

    private fun createViewModel(
        isEnabled: Boolean? = null,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) {
        viewModel = ScheduledImportInfoViewModel(
            savedState = savedState,
            scheduledImportRepository = scheduledImportRepository,
        )
        isEnabled?.let(viewModel::show)
    }

    @Test
    fun `given shown state, when recreated, then visibility and enabled value are restored`() = testBlocking {
        val savedState = SavedStateHandle()
        createViewModel(isEnabled = true, savedState = savedState)

        createViewModel(savedState = savedState)

        assertThat(viewModel.viewState.value.isVisible).isTrue()
        assertThat(viewModel.viewState.value.isEnabled).isTrue()
        assertThat(viewModel.viewState.value.isUpdating).isFalse()
        assertThat(viewModel.viewState.value.hasError).isFalse()
    }

    @Test
    fun `given dismissed state, when recreated, then the sheet stays hidden`() = testBlocking {
        val savedState = SavedStateHandle()
        createViewModel(isEnabled = false, savedState = savedState)
        viewModel.onDismissed()

        createViewModel(savedState = savedState)

        assertThat(viewModel.viewState.value.isVisible).isFalse()
        assertThat(viewModel.viewState.value.isEnabled).isFalse()
    }

    @Test
    fun `given an update is in progress, when recreated, then the confirmed value is restored`() = testBlocking {
        val savedState = SavedStateHandle()
        createViewModel(isEnabled = false, savedState = savedState)
        val gate = CompletableDeferred<WooResult<Boolean>>()
        whenever(scheduledImportRepository.setEnabled(true)).doSuspendableAnswer { gate.await() }
        viewModel.onOptionSelected(true)

        createViewModel(savedState = savedState)

        assertThat(viewModel.viewState.value.isVisible).isTrue()
        assertThat(viewModel.viewState.value.isEnabled).isFalse()
        assertThat(viewModel.viewState.value.isUpdating).isFalse()
        assertThat(viewModel.viewState.value.hasError).isFalse()
        gate.cancel()
    }

    @Test
    fun `when toggle is changed and update succeeds, then state reflects the new value and dismissal is requested`() =
        testBlocking {
            val savedState = SavedStateHandle()
            createViewModel(isEnabled = false, savedState = savedState)
            whenever(scheduledImportRepository.setEnabled(true)).thenReturn(WooResult(true))

            viewModel.onOptionSelected(true)

            val state = viewModel.viewState.value
            assertThat(state.isVisible).isTrue()
            assertThat(state.isEnabled).isTrue()
            assertThat(state.isDismissRequested).isTrue()
            assertThat(state.isUpdating).isFalse()
            assertThat(state.hasError).isFalse()

            createViewModel(savedState = savedState)

            assertThat(viewModel.viewState.value.isEnabled).isTrue()
            assertThat(viewModel.viewState.value.isDismissRequested).isFalse()
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

        val state = viewModel.viewState.value
        assertThat(state.isEnabled).isFalse()
        assertThat(state.isDismissRequested).isFalse()
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
            assertThat(viewModel.viewState.value.isUpdating).isTrue()

            // Second toggle while the first is still in flight must be ignored
            viewModel.onOptionSelected(false)
            gate.complete(WooResult(true))

            verify(scheduledImportRepository).setEnabled(true)
            verify(scheduledImportRepository, never()).setEnabled(false)
        }

    @Test
    fun `given an update is in progress, when dismissed, then the update is cancelled and state is reset`() =
        testBlocking {
            createViewModel(isEnabled = false)
            lateinit var updateContinuation: Continuation<WooResult<Boolean>>
            whenever(scheduledImportRepository.setEnabled(true)).doSuspendableAnswer {
                suspendCoroutine { updateContinuation = it }
            }

            viewModel.onOptionSelected(true)
            viewModel.onDismissed()
            updateContinuation.resumeWith(Result.success(WooResult(true)))
            runCurrent()

            assertThat(viewModel.viewState.value.isVisible).isFalse()
            assertThat(viewModel.viewState.value.isEnabled).isFalse()
            assertThat(viewModel.viewState.value.isDismissRequested).isFalse()
            assertThat(viewModel.viewState.value.isUpdating).isFalse()
            assertThat(viewModel.viewState.value.hasError).isFalse()
        }

    @Test
    fun `given an update fails, when recreated, then confirmed value is restored without transient state`() =
        testBlocking {
            val savedState = SavedStateHandle()
            createViewModel(isEnabled = false, savedState = savedState)
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

            createViewModel(savedState = savedState)

            assertThat(viewModel.viewState.value.isVisible).isTrue()
            assertThat(viewModel.viewState.value.isEnabled).isFalse()
            assertThat(viewModel.viewState.value.isUpdating).isFalse()
            assertThat(viewModel.viewState.value.hasError).isFalse()
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
    fun `given an option is already selected, when tapped, then dismissal is requested without a repository call`() =
        testBlocking {
            createViewModel(isEnabled = true)

            viewModel.onOptionSelected(true)
            viewModel.onOptionSelected(false)

            assertThat(viewModel.viewState.value.isDismissRequested).isTrue()
            verify(scheduledImportRepository, never()).setEnabled(true)
            verify(scheduledImportRepository, never()).setEnabled(false)
        }
}
