package com.woocommerce.android.ui.orders.creation

import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

interface CodeScanner {
    fun startScan() : Flow<CodeScannerStatus>
}

class GoogleCodeScanner @Inject constructor(private val scanner: GmsBarcodeScanner) : CodeScanner {
    override fun startScan(): Flow<CodeScannerStatus> {
        return callbackFlow {
            scanner.startScan()
                .addOnSuccessListener { code ->
                    this@callbackFlow.trySend(CodeScannerStatus.Success(code.rawValue))
                    this@callbackFlow.close()
                }
                .addOnFailureListener { throwable ->
                    this@callbackFlow.trySend(CodeScannerStatus.Failure(throwable.cause))
                    this@callbackFlow.close()
                }
            awaitClose()
        }
    }
}

sealed class CodeScannerStatus {
    data class Success(val code: String?) : CodeScannerStatus()
    data class Failure(val error: Throwable?) : CodeScannerStatus()
}
