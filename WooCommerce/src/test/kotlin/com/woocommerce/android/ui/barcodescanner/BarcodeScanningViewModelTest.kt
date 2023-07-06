package com.woocommerce.android.ui.barcodescanner

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.Granted
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.PermanentlyDenied
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.ShouldShowRationale
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class BarcodeScanningViewModelTest : BaseUnitTest() {
    private lateinit var barcodeScanningViewModel: BarcodeScanningViewModel
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setup() {
        savedStateHandle = mock()
        barcodeScanningViewModel = BarcodeScanningViewModel(savedStateHandle)
    }

    @Test
    fun `given permanently denied dialog shown, when user navigates back from the app settings, then launch the camera permission again`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )
        (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).ctaAction.invoke()

        barcodeScanningViewModel.onResume()

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(LaunchCameraPermission::class.java)
    }

    @Test
    fun `given permanently denied dialog shown, when user minimises and navigates back to our app without clicking the CTA, then do not launch the camera permission again`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        barcodeScanningViewModel.onResume()

        assertThat(barcodeScanningViewModel.event.value).isNull()
    }

    @Test
    fun `given denied dialog shown, when user minimises and navigates back to our app, then do not launch the camera permission again`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        barcodeScanningViewModel.onResume()

        assertThat(barcodeScanningViewModel.event.value).isNull()
    }

    @Test
    fun `given camera permission granted, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = true,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isEqualTo(Granted)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isInstanceOf(ShouldShowRationale::class.java)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then dialog title is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).title
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_title)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then dialog message is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).message
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_rationale_message)
    }

    @Test
    fun `given camera permission not granted and should show rationale, then cta label is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).ctaLabel
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_rationale_cta_label)
    }

    @Test
    fun `given camera permission not granted and should show rationale, when the CTA is clicked, then trigger proper event`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = true
        )

        (barcodeScanningViewModel.permissionState.value as ShouldShowRationale).ctaAction.invoke()

        assertThat(barcodeScanningViewModel.event.value).isInstanceOf(LaunchCameraPermission::class.java)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then update the PermissionState accordingly`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(barcodeScanningViewModel.permissionState.value).isInstanceOf(PermanentlyDenied::class.java)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then dialog title is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).title
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_title)
    }

    @Test
    fun `given camera permission not granted and should not show rationale, then dialog message is correct`() {
        barcodeScanningViewModel.updatePermissionState(
            isPermissionGranted = false,
            shouldShowRequestPermissionRationale = false
        )

        assertThat(
            (barcodeScanningViewModel.permissionState.value as PermanentlyDenied).message
        ).isEqualTo(R.string.barcode_scanning_alert_dialog_permanently_denied_message)
    }
}
