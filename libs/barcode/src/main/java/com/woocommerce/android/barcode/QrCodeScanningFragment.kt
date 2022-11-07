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

package com.woocommerce.android.barcode

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.woocommerce.android.barcode.camera.CameraSource
import com.woocommerce.android.barcode.camera.WorkflowModel
import com.woocommerce.android.barcode.camera.WorkflowModel.WorkflowState
import com.woocommerce.android.barcode.detection.BarcodeProcessor
import kotlinx.android.synthetic.main.camera_preview_overlay.graphicOverlay
import kotlinx.android.synthetic.main.camera_preview_overlay.promptChip
import kotlinx.android.synthetic.main.fragment_live_barcode.preview
import kotlinx.android.synthetic.main.top_action_bar_in_live_camera.closeButton
import kotlinx.android.synthetic.main.top_action_bar_in_live_camera.helpButton
import java.io.IOException

/** Demonstrates the barcode scanning workflow using camera preview.  */
class QrCodeScanningFragment : Fragment(), OnClickListener {
    companion object {
        const val TAG = "code_scanner_fragment"
        const val MAGIC_LOGIN_ACTION = "magic-login"
        const val MAGIC_LOGIN_SCHEME = "woocommerce"
    }

    private var cameraSource: CameraSource? = null
    private val workflowModel: WorkflowModel by activityViewModels()
    private var currentWorkflowState: WorkflowState? = null
    private var onCodeScanned: (rawValue: String) -> Unit = {}
    private var onHelpClicked: () -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_live_barcode, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        graphicOverlay.apply {
            setOnClickListener(this@QrCodeScanningFragment)
            cameraSource = CameraSource(this)
        }
        closeButton.setOnClickListener(this)
        helpButton.setOnClickListener { onHelpClicked() }
        setUpWorkflowModel()
    }

    override fun onResume() {
        super.onResume()

        workflowModel.markCameraFrozen()
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(BarcodeProcessor(graphicOverlay, workflowModel))
        workflowModel.setWorkflowState(WorkflowState.DETECTING)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource?.release()
        cameraSource = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.closeButton -> requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setClickListeners(
        onCodeScanned: (rawValue: String?) -> Unit,
        onHelpClicked: () -> Unit
    ) {
        this.onCodeScanned = onCodeScanned
        this.onHelpClicked = onHelpClicked
    }

    private fun startCameraPreview() {
        val workflowModel = this.workflowModel
        val cameraSource = this.cameraSource ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        val workflowModel = this.workflowModel
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            preview.stop()
        }
    }

    private fun setUpWorkflowModel() {
        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel.workflowState.observe(viewLifecycleOwner) { workflowState ->
            if (workflowState == null || currentWorkflowState == workflowState) {
                return@observe
            }

            currentWorkflowState = workflowState
            Log.d(TAG, "Current workflow state: ${currentWorkflowState!!.name}")

            when (workflowState) {
                WorkflowState.DETECTING -> {
                    promptChip.visibility = View.VISIBLE
                    promptChip.setText(R.string.prompt_point_at_a_barcode)
                    startCameraPreview()
                }
                WorkflowState.CONFIRMING -> {
                    promptChip.visibility = View.VISIBLE
                    promptChip.setText(R.string.prompt_move_camera_closer)
                    startCameraPreview()
                }
                WorkflowState.SEARCHING -> {
                    promptChip.visibility = View.VISIBLE
                    promptChip.setText(R.string.prompt_searching)
                    stopCameraPreview()
                }
                WorkflowState.DETECTED, WorkflowState.SEARCHED -> {
                    promptChip.visibility = View.GONE
                    stopCameraPreview()
                }
                else -> promptChip.visibility = View.GONE
            }
        }

        workflowModel.detectedBarcode.observe(viewLifecycleOwner) { barcode ->
            val rawValue = barcode.rawValue
            if (isValidScannedRawValue(rawValue)) {
                onCodeScanned(rawValue!!)
            } else {
                workflowModel.setWorkflowState(WorkflowState.DETECTING)
                Toast.makeText(
                    requireContext(), resources.getText(R.string.not_a_valid_qr_code), Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun isValidScannedRawValue(scannedRawValue: String?): Boolean =
        scannedRawValue != null &&
            scannedRawValue.contains(MAGIC_LOGIN_ACTION) &&
            scannedRawValue.contains(MAGIC_LOGIN_SCHEME)
}
