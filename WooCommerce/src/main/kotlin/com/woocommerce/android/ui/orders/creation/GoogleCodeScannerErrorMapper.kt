package com.woocommerce.android.ui.orders.creation

import com.google.mlkit.common.MlKitException
import javax.inject.Inject

class GoogleCodeScannerErrorMapper @Inject constructor() {

    fun mapGoogleMLKitScanningErrors(
        exception: MlKitException?
    ): CodeScanningErrorType {
        return when (exception?.errorCode) {
            ABORTED -> CodeScanningErrorType.Aborted
            ALREADY_EXISTS -> CodeScanningErrorType.AlreadyExists
            CANCELLED -> CodeScanningErrorType.Cancelled
            CODE_SCANNER_APP_NAME_UNAVAILABLE -> CodeScanningErrorType.CodeScannerAppNameUnavailable
            CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED ->
                CodeScanningErrorType.CodeScannerCameraPermissionNotGranted
            CODE_SCANNER_CANCELLED -> CodeScanningErrorType.CodeScannerCancelled
            CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD ->
                CodeScanningErrorType.CodeScannerGooglePlayServicesVersionTooOld
            CODE_SCANNER_PIPELINE_INFERENCE_ERROR -> CodeScanningErrorType.CodeScannerPipelineInferenceError
            CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR ->
                CodeScanningErrorType.CodeScannerPipelineInitializationError
            CODE_SCANNER_TASK_IN_PROGRESS -> CodeScanningErrorType.CodeScannerTaskInProgress
            CODE_SCANNER_UNAVAILABLE -> CodeScanningErrorType.CodeScannerUnavailable
            DATA_LOSS -> CodeScanningErrorType.DataLoss
            DEADLINE_EXCEEDED -> CodeScanningErrorType.DeadlineExceeded
            FAILED_PRECONDITION -> CodeScanningErrorType.FailedPrecondition
            INTERNAL -> CodeScanningErrorType.Internal
            INVALID_ARGUMENT -> CodeScanningErrorType.InvalidArgument
            MODEL_HASH_MISMATCH -> CodeScanningErrorType.ModelHashMismatch
            MODEL_INCOMPATIBLE_WITH_TFLITE -> CodeScanningErrorType.ModelIncompatibleWithTFLite
            NETWORK_ISSUE -> CodeScanningErrorType.NetworkIssue
            NOT_ENOUGH_SPACE -> CodeScanningErrorType.NotEnoughSpace
            NOT_FOUND -> CodeScanningErrorType.NotFound
            OUT_OF_RANGE -> CodeScanningErrorType.OutOfRange
            PERMISSION_DENIED -> CodeScanningErrorType.PermissionDenied
            RESOURCE_EXHAUSTED -> CodeScanningErrorType.ResourceExhausted
            UNAUTHENTICATED -> CodeScanningErrorType.UnAuthenticated
            UNAVAILABLE -> CodeScanningErrorType.UnAvailable
            UNIMPLEMENTED -> CodeScanningErrorType.UnImplemented
            UNKNOWN -> CodeScanningErrorType.Unknown
            else -> CodeScanningErrorType.Other
        }
    }

    private companion object {
        private const val ABORTED = 10
        private const val ALREADY_EXISTS = 6
        private const val CANCELLED = 1
        private const val CODE_SCANNER_APP_NAME_UNAVAILABLE = 203
        private const val CODE_SCANNER_CAMERA_PERMISSION_NOT_GRANTED = 202
        private const val CODE_SCANNER_CANCELLED = 201
        private const val CODE_SCANNER_GOOGLE_PLAY_SERVICES_VERSION_TOO_OLD = 207
        private const val CODE_SCANNER_PIPELINE_INFERENCE_ERROR = 206
        private const val CODE_SCANNER_PIPELINE_INITIALIZATION_ERROR = 205
        private const val CODE_SCANNER_TASK_IN_PROGRESS = 204
        private const val CODE_SCANNER_UNAVAILABLE = 200
        private const val DATA_LOSS = 15
        private const val DEADLINE_EXCEEDED = 4
        private const val FAILED_PRECONDITION = 9
        private const val INTERNAL = 13
        private const val INVALID_ARGUMENT = 3
        private const val MODEL_HASH_MISMATCH = 102
        private const val MODEL_INCOMPATIBLE_WITH_TFLITE = 100
        private const val NETWORK_ISSUE = 17
        private const val NOT_ENOUGH_SPACE = 101
        private const val NOT_FOUND = 5
        private const val OUT_OF_RANGE = 11
        private const val PERMISSION_DENIED = 7
        private const val RESOURCE_EXHAUSTED = 8
        private const val UNAUTHENTICATED = 16
        private const val UNAVAILABLE = 14
        private const val UNIMPLEMENTED = 12
        private const val UNKNOWN = 2
    }
}
