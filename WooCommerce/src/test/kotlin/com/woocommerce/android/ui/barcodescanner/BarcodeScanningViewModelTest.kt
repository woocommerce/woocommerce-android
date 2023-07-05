package com.woocommerce.android.ui.barcodescanner

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.PermanentlyDenied
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
}
