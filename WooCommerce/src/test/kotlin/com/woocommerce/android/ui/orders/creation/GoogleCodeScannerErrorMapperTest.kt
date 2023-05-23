package com.woocommerce.android.ui.orders.creation

import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.MlKitException.ABORTED
import com.google.mlkit.common.MlKitException.ALREADY_EXISTS
import com.google.mlkit.common.MlKitException.CANCELLED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_APP_NAME_UNAVAILABLE
import com.google.mlkit.common.MlKitException.CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_CANCELLED
import com.google.mlkit.common.MlKitException.CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD
import com.google.mlkit.common.MlKitException.CODE_SCANNER_PIPELINE_INFERENCE_ERROR
import com.google.mlkit.common.MlKitException.CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR
import com.google.mlkit.common.MlKitException.CODE_SCANNER_TASK_IN_PROGRESS
import com.google.mlkit.common.MlKitException.CODE_SCANNER_UNAVAILABLE
import com.google.mlkit.common.MlKitException.DATA_LOSS
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GoogleCodeScannerErrorMapperTest : BaseUnitTest() {
    private lateinit var mapper: GoogleCodeScannerErrorMapper

    private val mlKitException =  mock<MlKitException>().also {
        whenever(it.errorCode).thenReturn(CODE_SCANNER_CANCELLED)
    }

    @Before
    fun setup() {
        mapper = GoogleCodeScannerErrorMapper()
    }

    @Test
    fun `when ABORTED exception thrown, then Aborted type returned`() {
        whenever(mlKitException.errorCode).thenReturn(ABORTED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Aborted
        )
    }

    @Test
    fun `when ALREADY_EXISTS exception thrown, then AlreadyExists type returned`() {
        whenever(mlKitException.errorCode).thenReturn(ALREADY_EXISTS)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.AlreadyExists
        )
    }

    @Test
    fun `when CANCELLED exception thrown, then Cancelled type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CANCELLED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Cancelled
        )
    }

    @Test
    fun `when CODE_SCANNER_APP_NAME_UNAVAILABLE exception thrown, then CodeScannerAppNameUnavailable type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_APP_NAME_UNAVAILABLE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerAppNameUnavailable
        )
    }

    @Test
    fun `when CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED exception thrown, then CodeScannerCameraPermissionNotGranted type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerCameraPermissionNotGranted
        )
    }

    @Test
    fun `when CODE_SCANNER_CANCELLED exception thrown, then CodeScannerCancelled type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_CANCELLED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerCancelled
        )
    }

    @Test
    fun `when CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD exception thrown, then CodeScannerGooglePlayServicesVersionTooOld type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerGooglePlayServicesVersionTooOld
        )
    }

    @Test
    fun `when CODE_SCANNER_PIPELINE_INFERENCE_ERROR exception thrown, then CodeScannerPipelineInferenceError type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_PIPELINE_INFERENCE_ERROR)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerPipelineInferenceError
        )
    }

    @Test
    fun `when CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR exception thrown, then CodeScannerPipelineInitializationError type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerPipelineInitializationError
        )
    }

    @Test
    fun `when CODE_SCANNER_TASK_IN_PROGRESS exception thrown, then CodeScannerTaskInProgress type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_TASK_IN_PROGRESS)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerTaskInProgress
        )
    }

    @Test
    fun `when CODE_SCANNER_UNAVAILABLE exception thrown, then CodeScannerUnavailable type returned`() {
        whenever(mlKitException.errorCode).thenReturn(CODE_SCANNER_UNAVAILABLE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.CodeScannerUnavailable
        )
    }

    @Test
    fun `when DATA_LOSS exception thrown, then DataLoss type returned`() {
        whenever(mlKitException.errorCode).thenReturn(DATA_LOSS)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.DataLoss
        )
    }
}
