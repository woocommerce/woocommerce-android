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
import com.google.mlkit.common.MlKitException.DEADLINE_EXCEEDED
import com.google.mlkit.common.MlKitException.FAILED_PRECONDITION
import com.google.mlkit.common.MlKitException.INTERNAL
import com.google.mlkit.common.MlKitException.INVALID_ARGUMENT
import com.google.mlkit.common.MlKitException.MODEL_HASH_MISMATCH
import com.google.mlkit.common.MlKitException.MODEL_INCOMPATIBLE_WITH_TFLITE
import com.google.mlkit.common.MlKitException.NETWORK_ISSUE
import com.google.mlkit.common.MlKitException.NOT_ENOUGH_SPACE
import com.google.mlkit.common.MlKitException.NOT_FOUND
import com.google.mlkit.common.MlKitException.OUT_OF_RANGE
import com.google.mlkit.common.MlKitException.PERMISSION_DENIED
import com.google.mlkit.common.MlKitException.RESOURCE_EXHAUSTED
import com.google.mlkit.common.MlKitException.UNAUTHENTICATED
import com.google.mlkit.common.MlKitException.UNAVAILABLE
import com.google.mlkit.common.MlKitException.UNIMPLEMENTED
import com.google.mlkit.common.MlKitException.UNKNOWN
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
    private val mlKitException = mock<MlKitException>().also {
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

    @Test
    fun `when DEADLINE_EXCEEDED exception thrown, then DeadlineExceeded type returned`() {
        whenever(mlKitException.errorCode).thenReturn(DEADLINE_EXCEEDED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.DeadlineExceeded
        )
    }

    @Test
    fun `when FAILED_PRECONDITION exception thrown, then FailedPrecondition type returned`() {
        whenever(mlKitException.errorCode).thenReturn(FAILED_PRECONDITION)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.FailedPrecondition
        )
    }

    @Test
    fun `when INTERNAL exception thrown, then Internal type returned`() {
        whenever(mlKitException.errorCode).thenReturn(INTERNAL)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Internal
        )
    }

    @Test
    fun `when INVALID_ARGUMENT exception thrown, then InvalidArgument type returned`() {
        whenever(mlKitException.errorCode).thenReturn(INVALID_ARGUMENT)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.InvalidArgument
        )
    }

    @Test
    fun `when MODEL_HASH_MISMATCH exception thrown, then ModelHashMismatch type returned`() {
        whenever(mlKitException.errorCode).thenReturn(MODEL_HASH_MISMATCH)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.ModelHashMismatch
        )
    }

    @Test
    fun `when MODEL_INCOMPATIBLE_WITH_TFLITE exception thrown, then ModelIncompatibleWithTFLite type returned`() {
        whenever(mlKitException.errorCode).thenReturn(MODEL_INCOMPATIBLE_WITH_TFLITE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.ModelIncompatibleWithTFLite
        )
    }

    @Test
    fun `when NETWORK_ISSUE exception thrown, then NetworkIssue type returned`() {
        whenever(mlKitException.errorCode).thenReturn(NETWORK_ISSUE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.NetworkIssue
        )
    }

    @Test
    fun `when NOT_ENOUGH_SPACE exception thrown, then NotEnoughSpace type returned`() {
        whenever(mlKitException.errorCode).thenReturn(NOT_ENOUGH_SPACE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.NotEnoughSpace
        )
    }

    @Test
    fun `when NOT_FOUND exception thrown, then NotFound type returned`() {
        whenever(mlKitException.errorCode).thenReturn(NOT_FOUND)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.NotFound
        )
    }

    @Test
    fun `when OUT_OF_RANGE exception thrown, then OutOfRange type returned`() {
        whenever(mlKitException.errorCode).thenReturn(OUT_OF_RANGE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.OutOfRange
        )
    }

    @Test
    fun `when PERMISSION_DENIED exception thrown, then PermissionDenied type returned`() {
        whenever(mlKitException.errorCode).thenReturn(PERMISSION_DENIED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.PermissionDenied
        )
    }

    @Test
    fun `when RESOURCE_EXHAUSTED exception thrown, then ResourceExhausted type returned`() {
        whenever(mlKitException.errorCode).thenReturn(RESOURCE_EXHAUSTED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.ResourceExhausted
        )
    }

    @Test
    fun `when UNAUTHENTICATED exception thrown, then UnAuthenticated type returned`() {
        whenever(mlKitException.errorCode).thenReturn(UNAUTHENTICATED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.UnAuthenticated
        )
    }

    @Test
    fun `when UNAVAILABLE exception thrown, then UnAvailable type returned`() {
        whenever(mlKitException.errorCode).thenReturn(UNAVAILABLE)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.UnAvailable
        )
    }

    @Test
    fun `when UNIMPLEMENTED exception thrown, then UnImplemented type returned`() {
        whenever(mlKitException.errorCode).thenReturn(UNIMPLEMENTED)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.UnImplemented
        )
    }

    @Test
    fun `when UNKNOWN exception thrown, then Unknown type returned`() {
        whenever(mlKitException.errorCode).thenReturn(UNKNOWN)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Unknown
        )
    }

    @Test
    fun `when exception is null, then Other type returned`() {
        val mlKitException: MlKitException? = null

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isInstanceOf(
            CodeScanningErrorType.Other::class.java
        )
    }

    @Test
    fun `when exception is not MLKitException, then Other type returned with proper message`() {
        val mlKitException = Throwable("Barcode unrecognized")

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isEqualTo(
            CodeScanningErrorType.Other(mlKitException)
        )
    }

    @Test
    fun `when exception returns invalid error code, then Other type returned`() {
        whenever(mlKitException.errorCode).thenReturn(-1)

        assertThat(mapper.mapGoogleMLKitScanningErrors(mlKitException)).isInstanceOf(
            CodeScanningErrorType.Other::class.java
        )
    }
}
