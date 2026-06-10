package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.ui.login.qrlogin.QrLoginPrologueViewModel.Dispatch
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class QrLoginPrologueViewModelTest : BaseUnitTest() {

    private val unifiedLoginTracker: UnifiedLoginTracker = mock()

    private val viewModel by lazy {
        QrLoginPrologueViewModel(
            savedState = SavedStateHandle(),
            unifiedLoginTracker = unifiedLoginTracker,
        )
    }

    @Test
    fun `given prologue is shown, when tracked, then QR prologue step is reported`() = testBlocking {
        viewModel.onPrologueShown()

        verify(unifiedLoginTracker).track(Flow.LOGIN_QR, Step.QR_PROLOGUE)
    }

    // region scan

    @Test
    fun `given camera permission granted, when scan is clicked, then navigates to scanner`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onScanClicked(isCameraPermissionGranted = true)

        verify(unifiedLoginTracker).trackClick(Click.LOGIN_QR_SCAN)
        assertThat(events.last()).isEqualTo(Dispatch.NavigateToScanner)
    }

    @Test
    fun `given camera permission not granted, when scan is clicked, then permission request is launched`() =
        testBlocking {
            val events = viewModel.event.captureValues()

            viewModel.onScanClicked(isCameraPermissionGranted = false)

            inOrder(unifiedLoginTracker).apply {
                verify(unifiedLoginTracker).trackClick(Click.LOGIN_QR_SCAN)
                verify(unifiedLoginTracker).trackClick(Click.QR_CAMERA_PERMISSION_DIALOG_SHOWN)
            }
            assertThat(events.last()).isEqualTo(Dispatch.LaunchCameraPermissionRequest)
        }

    // endregion

    // region fallback and help

    @Test
    fun `when site address login is clicked, then navigates to site address login`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onSiteAddressLoginClicked()

        verify(unifiedLoginTracker).trackClick(Click.LOGIN_QR_FALLBACK)
        assertThat(events.last()).isEqualTo(Dispatch.NavigateToSiteAddressLogin)
    }

    @Test
    fun `when help is clicked, then navigates to help`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onHelpClicked()

        verify(unifiedLoginTracker).trackClick(Click.SHOW_HELP)
        assertThat(events.last()).isEqualTo(Dispatch.NavigateToHelp)
    }

    // endregion

    // region camera permission result

    @Test
    fun `given permission granted via result, when handled, then dialog hidden and navigates to scanner`() =
        testBlocking {
            val events = viewModel.event.captureValues()

            viewModel.onCameraPermissionResult(granted = true, shouldShowRationale = false)

            verify(unifiedLoginTracker).trackClick(Click.QR_CAMERA_PERMISSION_GRANTED)
            assertThat(viewModel.uiState.value.cameraPermissionDialog).isNull()
            assertThat(events.last()).isEqualTo(Dispatch.NavigateToScanner)
        }

    @Test
    fun `given permission denied with rationale, when handled, then first-denial dialog is shown`() = testBlocking {
        viewModel.onCameraPermissionResult(granted = false, shouldShowRationale = true)

        verify(unifiedLoginTracker).trackClick(Click.QR_CAMERA_PERMISSION_DENIED)
        val dialog = viewModel.uiState.value.cameraPermissionDialog
        assertThat(dialog).isNotNull
        assertThat(dialog?.title).isEqualTo(R.string.login_qr_prologue_camera_denied_title)
        assertThat(dialog?.body).isEqualTo(R.string.login_qr_prologue_camera_denied_body)
        assertThat(dialog?.primaryLabel).isEqualTo(R.string.login_qr_prologue_camera_denied_allow_button)
    }

    @Test
    fun `given permission denied without rationale, when handled, then permanently-denied dialog is shown`() =
        testBlocking {
            viewModel.onCameraPermissionResult(granted = false, shouldShowRationale = false)

            verify(unifiedLoginTracker).trackClick(Click.QR_CAMERA_PERMISSION_DENIED)
            val dialog = viewModel.uiState.value.cameraPermissionDialog
            assertThat(dialog).isNotNull
            assertThat(dialog?.title).isEqualTo(R.string.login_qr_prologue_camera_blocked_title)
            assertThat(dialog?.body).isEqualTo(R.string.login_qr_prologue_camera_blocked_body)
            assertThat(dialog?.primaryLabel).isEqualTo(R.string.login_qr_prologue_camera_blocked_settings_button)
        }

    // endregion

    // region denial dialog actions

    @Test
    fun `given first-denial dialog, when primary clicked, then dialog hidden and permission request relaunched`() =
        testBlocking {
            viewModel.onCameraPermissionResult(granted = false, shouldShowRationale = true)
            val events = viewModel.event.captureValues()

            viewModel.onCameraDenialPrimaryClicked()

            assertThat(viewModel.uiState.value.cameraPermissionDialog).isNull()
            verify(unifiedLoginTracker).trackClick(Click.QR_CAMERA_PERMISSION_DIALOG_SHOWN)
            assertThat(events.last()).isEqualTo(Dispatch.LaunchCameraPermissionRequest)
        }

    @Test
    fun `given permanently-denied dialog, when primary clicked, then dialog hidden and app settings opened`() =
        testBlocking {
            viewModel.onCameraPermissionResult(granted = false, shouldShowRationale = false)
            val events = viewModel.event.captureValues()

            viewModel.onCameraDenialPrimaryClicked()

            assertThat(viewModel.uiState.value.cameraPermissionDialog).isNull()
            assertThat(events.last()).isEqualTo(Dispatch.OpenAppSettings)
        }

    @Test
    fun `given no denial dialog, when primary clicked, then nothing happens`() = testBlocking {
        val events = viewModel.event.captureValues()

        viewModel.onCameraDenialPrimaryClicked()

        assertThat(viewModel.uiState.value.cameraPermissionDialog).isNull()
        assertThat(events).isEmpty()
    }

    @Test
    fun `given a denial dialog, when cancelled, then dialog is hidden`() = testBlocking {
        viewModel.onCameraPermissionResult(granted = false, shouldShowRationale = true)
        assertThat(viewModel.uiState.value.cameraPermissionDialog).isNotNull

        viewModel.onCameraDenialCancelled()

        assertThat(viewModel.uiState.value.cameraPermissionDialog).isNull()
    }

    // endregion
}
