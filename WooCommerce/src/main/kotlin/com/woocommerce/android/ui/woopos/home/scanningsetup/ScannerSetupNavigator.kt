package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScannerConfigurations
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import javax.inject.Inject

class ScannerSetupNavigator @Inject constructor() {
    fun getNextStep(device: BarcodeReaderDevice, currentStep: ScanningSetupStep): ScanningSetupStep {
        val stepSequence = ScannerConfigurations.getStepSequence(device)
        val currentIndex = stepSequence.indexOf(currentStep)

        if (currentIndex == -1) {
            error("Current step not found in configuration for device: $device, current step: $currentStep")
        }

        val nextIndex = currentIndex + 1
        if (nextIndex >= stepSequence.size) {
            error("No next step available for device: $device, current step: $currentStep")
        }

        return stepSequence[nextIndex]
    }

    fun getPreviousStep(device: BarcodeReaderDevice, currentStep: ScanningSetupStep): ScanningSetupStep {
        val stepSequence = ScannerConfigurations.getStepSequence(device)
        val currentIndex = stepSequence.indexOf(currentStep)

        if (currentIndex <= 0) {
            return ScanningSetupStep.DeviceSelection
        }

        val previousIndex = currentIndex - 1
        return stepSequence[previousIndex]
    }
}
