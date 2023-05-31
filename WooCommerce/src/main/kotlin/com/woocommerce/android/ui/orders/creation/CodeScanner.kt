package com.woocommerce.android.ui.orders.creation

import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface CodeScanner {
    fun startScan(): Flow<CodeScannerStatus>
}

class GoogleCodeScanner @Inject constructor(
    private val scanner: GmsBarcodeScanner,
    private val errorMapper: GoogleCodeScannerErrorMapper,
    private val barcodeFormatMapper: GoogleBarcodeFormatMapper,
) : CodeScanner {
    override fun startScan(): Flow<CodeScannerStatus> {
        return callbackFlow {
            scanner.startScan()
                .addOnSuccessListener { code ->
                    handleScanSuccess(code)
                    this@callbackFlow.close()
                }
                .addOnFailureListener { throwable ->
                    this@callbackFlow.trySend(
                        CodeScannerStatus.Failure(
                            error = throwable.message,
                            type = errorMapper.mapGoogleMLKitScanningErrors(throwable)
                        )
                    )
                    this@callbackFlow.close()
                }
            awaitClose()
        }
    }

    private fun ProducerScope<CodeScannerStatus>.handleScanSuccess(code: Barcode) {
        code.rawValue?.let {
            trySend(
                CodeScannerStatus.Success(
                    it,
                    barcodeFormatMapper.mapBarcodeFormat(code.format)
                )
            )
        } ?: run {
            trySend(
                CodeScannerStatus.Failure(
                    error = "Failed to find a valid raw value!",
                    type = CodeScanningErrorType.Other(Throwable("Empty raw value"))
                )
            )
        }
    }
}

sealed class CodeScannerStatus {
    data class Success(val code: String, val format: BarcodeFormat) : CodeScannerStatus()
    data class Failure(
        val error: String?,
        val type: CodeScanningErrorType
    ) : CodeScannerStatus()
}

sealed class CodeScanningErrorType {
    object Aborted : CodeScanningErrorType()
    object AlreadyExists : CodeScanningErrorType()
    object Cancelled : CodeScanningErrorType()
    object CodeScannerAppNameUnavailable : CodeScanningErrorType()
    object CodeScannerCameraPermissionNotGranted : CodeScanningErrorType()
    object CodeScannerCancelled : CodeScanningErrorType()
    object CodeScannerGooglePlayServicesVersionTooOld : CodeScanningErrorType()
    object CodeScannerPipelineInferenceError : CodeScanningErrorType()
    object CodeScannerPipelineInitializationError : CodeScanningErrorType()
    object CodeScannerTaskInProgress : CodeScanningErrorType()
    object CodeScannerUnavailable : CodeScanningErrorType()
    object DataLoss : CodeScanningErrorType()
    object DeadlineExceeded : CodeScanningErrorType()
    object FailedPrecondition : CodeScanningErrorType()
    object Internal : CodeScanningErrorType()
    object InvalidArgument : CodeScanningErrorType()
    object ModelHashMismatch : CodeScanningErrorType()
    object ModelIncompatibleWithTFLite : CodeScanningErrorType()
    object NetworkIssue : CodeScanningErrorType()
    object NotEnoughSpace : CodeScanningErrorType()
    object NotFound : CodeScanningErrorType()
    object OutOfRange : CodeScanningErrorType()
    object PermissionDenied : CodeScanningErrorType()
    object ResourceExhausted : CodeScanningErrorType()
    object UnAuthenticated : CodeScanningErrorType()
    object UnAvailable : CodeScanningErrorType()
    object UnImplemented : CodeScanningErrorType()
    object Unknown : CodeScanningErrorType()
    data class Other(val throwable: Throwable?) : CodeScanningErrorType()

    override fun toString(): String = when (this) {
        is Other -> this.throwable?.message ?: "Other"
        else -> this.javaClass.run {
            name.removePrefix("${`package`?.name ?: ""}.")
        }
    }
}
