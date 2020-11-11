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

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.woocommerce.android.barcode.camera.WorkflowModel
import com.woocommerce.android.barcode.camera.WorkflowModel.WorkflowState
import com.woocommerce.android.barcode.camera.CameraSource
import com.woocommerce.android.barcode.detection.BarcodeField
import com.woocommerce.android.barcode.detection.BarcodeProcessor
import com.woocommerce.android.barcode.detection.BarcodeResultFragment
import kotlinx.android.synthetic.main.camera_preview_overlay.*
import kotlinx.android.synthetic.main.fragment_live_barcode.*
import kotlinx.android.synthetic.main.top_action_bar_in_live_camera.*
import java.io.IOException
import java.util.ArrayList

/** Demonstrates the barcode scanning workflow using camera preview.  */
class LiveBarcodeScanningFragment : Fragment(), OnClickListener {
    private var cameraSource: CameraSource? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowState? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        graphicOverlay.apply {
            setOnClickListener(this@LiveBarcodeScanningFragment)
            cameraSource = CameraSource(this)
        }

        promptChipAnimator = (AnimatorInflater.loadAnimator(
            requireContext(),
            R.animator.bottom_prompt_chip_enter
        ) as AnimatorSet).apply {
            setTarget(promptChip)
        }

        closeButton.setOnClickListener(this)

        flashButton.apply {
            setOnClickListener(this@LiveBarcodeScanningFragment)
        }
        settingsButton.apply {
            setOnClickListener(this@LiveBarcodeScanningFragment)
        }

        setUpWorkflowModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_live_barcode, container, false)
    }

    override fun onResume() {
        super.onResume()

        workflowModel?.markCameraFrozen()
        settingsButton.isEnabled = true
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(BarcodeProcessor(graphicOverlay, workflowModel!!))
        workflowModel?.setWorkflowState(WorkflowState.DETECTING)
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
            R.id.closeButton -> requireActivity().onBackPressed()
            R.id.flashButton -> {
                flashButton.let {
                    if (it.isSelected) {
                        it.isSelected = false
                        cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                    } else {
                        it.isSelected = true
                        cameraSource!!.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                    }
                }
            }
            R.id.settingsButton -> {
                settingsButton.isEnabled = false
            }
        }
    }

    private fun startCameraPreview() {
        val workflowModel = this.workflowModel ?: return
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
        val workflowModel = this.workflowModel ?: return
        if (workflowModel.isCameraLive) {
            workflowModel.markCameraFrozen()
            flashButton.isSelected = false
            preview.stop()
        }
    }

    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java)

        // Observes the workflow state changes, if happens, update the overlay view indicators and
        // camera preview state.
        workflowModel!!.workflowState.observe(viewLifecycleOwner, Observer { workflowState ->
            if (workflowState == null || currentWorkflowState == workflowState) {
                return@Observer
            }

            currentWorkflowState = workflowState
            Log.d(TAG, "Current workflow state: ${currentWorkflowState!!.name}")

            val wasPromptChipGone = promptChip.visibility == View.GONE

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

            val shouldPlayPromptChipEnteringAnimation = wasPromptChipGone && promptChip.visibility == View.VISIBLE
            promptChipAnimator?.let {
                if (shouldPlayPromptChipEnteringAnimation && !it.isRunning) it.start()
            }
        })

        workflowModel?.detectedBarcode?.observe(viewLifecycleOwner, Observer { barcode ->
            if (barcode != null) {
                val barcodeFieldList = ArrayList<BarcodeField>()
                barcodeFieldList.add(BarcodeField("Raw Value", barcode.rawValue ?: ""))
                BarcodeResultFragment.show(childFragmentManager, barcodeFieldList)
            }
        })
    }

    companion object {
        private const val TAG = "LiveBarcodeActivity"
    }
}
