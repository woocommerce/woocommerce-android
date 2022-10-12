/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.woocommerce.android.barcode.detection

import android.animation.ValueAnimator
import android.util.Log
import androidx.annotation.MainThread
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.woocommerce.android.barcode.camera.CameraReticleAnimator
import com.woocommerce.android.barcode.camera.FrameProcessorBase
import com.woocommerce.android.barcode.camera.GraphicOverlay
import com.woocommerce.android.barcode.camera.WorkflowModel
import com.woocommerce.android.barcode.camera.WorkflowModel.WorkflowState
import java.io.IOException

/** A processor to run the barcode detector.  */
class BarcodeProcessor(
    graphicOverlay: GraphicOverlay,
    private val workflowModel: WorkflowModel
) : FrameProcessorBase<List<Barcode>>() {
    companion object {
        private const val TAG = "BarcodeProcessor"
        const val ANIMATION_DURATION_IN_MILLIS = 2000L
    }

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    private val cameraReticleAnimator: CameraReticleAnimator = CameraReticleAnimator(graphicOverlay)

    override fun detectInImage(image: InputImage): Task<List<Barcode>> =
        scanner.process(image)

    @MainThread
    override fun onSuccess(
        results: List<Barcode>,
        graphicOverlay: GraphicOverlay
    ) {
        if (!workflowModel.isCameraLive) return

        Log.d(TAG, "Barcode result size: ${results.size}")

        // Picks the barcode, if exists, that covers the center of graphic overlay.
        val barcodeInCenter = results.firstOrNull { barcode ->
            val boundingBox = barcode.boundingBox ?: return@firstOrNull false
            val box = graphicOverlay.translateRect(boundingBox)
            box.contains(graphicOverlay.width / 2f, graphicOverlay.height / 2f)
        }

        graphicOverlay.clear()
        if (barcodeInCenter == null) {
            cameraReticleAnimator.start()
            graphicOverlay.add(BarcodeReticleGraphic(graphicOverlay, cameraReticleAnimator))
            workflowModel.setWorkflowState(WorkflowState.DETECTING)
        } else {
            cameraReticleAnimator.cancel()
            val loadingAnimator = createLoadingAnimator(graphicOverlay, barcodeInCenter)
            loadingAnimator.start()
            graphicOverlay.add(BarcodeLoadingGraphic(graphicOverlay, loadingAnimator))
            workflowModel.setWorkflowState(WorkflowState.SEARCHING)
        }
        graphicOverlay.invalidate()
    }

    @SuppressWarnings("MagicNumber")
    private fun createLoadingAnimator(graphicOverlay: GraphicOverlay, barcode: Barcode): ValueAnimator {
        val endProgress = 1.1f
        return ValueAnimator.ofFloat(0f, endProgress).apply {
            duration = ANIMATION_DURATION_IN_MILLIS
            addUpdateListener {
                if ((animatedValue as Float).compareTo(endProgress) >= 0) {
                    graphicOverlay.clear()
                    workflowModel.setWorkflowState(WorkflowState.SEARCHED)
                    workflowModel.detectedBarcode.setValue(barcode)
                } else {
                    graphicOverlay.invalidate()
                }
            }
        }
    }

    override fun onFailure(e: Exception) {
        Log.e(TAG, "Barcode detection failed!", e)
    }

    override fun stop() {
        super.stop()
        try {
            scanner.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close barcode detector!", e)
        }
    }
}
