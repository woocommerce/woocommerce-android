package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.ScanningSetupStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosScannerSetupNavigatorTest {
    private val navigator = WooPosScannerSetupNavigator()

    @Test
    fun `given getInitialStep called, when invoked, then should return DeviceSelection`() {
        // WHEN
        val initialStep = navigator.getInitialStep()

        // THEN
        assertThat(initialStep).isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
    }

    @Test
    fun `given DeviceSelection step, when TERA_1200 selected, then should navigate to ScannerHIDModeSetup`() {
        // GIVEN
        val deviceSelectionStep = ScanningSetupStep.DeviceSelection

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.TERA_1200, deviceSelectionStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
        assertThat((nextStep as ScanningSetupStep.ScannerHIDModeSetup).qrCodeImageRes).isEqualTo(R.drawable.ic_barcode)
    }

    @Test
    fun `given ScannerHIDModeSetup step, when TERA_1200 device, then should navigate to ScannerPairModeSetup`() {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.TERA_1200, hidModeStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
    }

    @Test
    fun `given ScannerPairModeSetup step, when TERA_1200 device, then should navigate to PairYourScanner`() {
        // GIVEN
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup()

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairModeStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given PairYourScanner step, when TERA_1200 device, then should navigate to TestYourScanner`() {
        // GIVEN
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.TERA_1200, pairYourScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given TestYourScanner step, when TERA_1200 device, then should navigate to ScannerSetupSuccess`() {
        // GIVEN
        val testScannerStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.TERA_1200, testScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerSetupSuccess::class.java)
    }

    @Test
    fun `given DeviceSelection step, when STAR_BSH_20B selected, then should navigate to ScannerHIDModeSetup`() {
        // GIVEN
        val deviceSelectionStep = ScanningSetupStep.DeviceSelection

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, deviceSelectionStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
    }

    @Test
    fun `given ScannerHIDModeSetup step, when STAR_BSH_20B device, then should navigate directly to PairYourScanner`() {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.star_bsh_20b_setup_qr)

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, hidModeStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given PairYourScanner step, when STAR_BSH_20B device, then should navigate to TestYourScanner`() {
        // GIVEN
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, pairYourScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given TestYourScanner step, when STAR_BSH_20B device, then should navigate to ScannerSetupSuccess`() {
        // GIVEN
        val testScannerStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.STAR_BSH_20B, testScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerSetupSuccess::class.java)
    }

    @Test
    fun `given DeviceSelection step, when INATECK_BLUETOOTH selected, then should navigate to ScannerHIDModeSetup`() {
        // GIVEN
        val deviceSelectionStep = ScanningSetupStep.DeviceSelection

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.INATECK_BLUETOOTH, deviceSelectionStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
        assertThat((nextStep as ScanningSetupStep.ScannerHIDModeSetup).qrCodeImageRes).isEqualTo(R.drawable.ic_barcode)
    }

    @Test
    fun `given ScannerHIDModeSetup step, when INATECK_BLUETOOTH device, then should navigate to ScannerPairModeSetup`() {
        // GIVEN
        val hidModeStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.INATECK_BLUETOOTH, hidModeStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
    }

    @Test
    fun `given ScannerPairModeSetup step, when INATECK_BLUETOOTH device, then should navigate to PairYourScanner`() {
        // GIVEN
        val pairModeStep = ScanningSetupStep.ScannerPairModeSetup()

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.INATECK_BLUETOOTH, pairModeStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given PairYourScanner step, when INATECK_BLUETOOTH device, then should navigate to TestYourScanner`() {
        // GIVEN
        val pairYourScannerStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.INATECK_BLUETOOTH, pairYourScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given TestYourScanner step, when INATECK_BLUETOOTH device, then should navigate to ScannerSetupSuccess`() {
        // GIVEN
        val testScannerStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.INATECK_BLUETOOTH, testScannerStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerSetupSuccess::class.java)
    }

    @Test
    fun `given DeviceSelection step, when OTHER selected, then should navigate to ScannerSetupInfo`() {
        // GIVEN
        val deviceSelectionStep = ScanningSetupStep.DeviceSelection

        // WHEN
        val nextStep = navigator.getNextStep(BarcodeReaderDevice.OTHER, deviceSelectionStep)

        // THEN
        assertThat(nextStep).isInstanceOf(ScanningSetupStep.ScannerSetupInfo::class.java)
    }

    @Test
    fun `given ScannerHIDModeSetup step, when getting previous step, then should return DeviceSelection`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerHIDModeSetup(qrCodeImageRes = R.drawable.ic_barcode)

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
    }

    @Test
    fun `given ScannerPairModeSetup step, when getting previous step for TERA_1200, then should return ScannerHIDModeSetup`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerPairModeSetup()

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
    }

    @Test
    fun `given PairYourScanner step, when getting previous step for TERA_1200, then should return ScannerPairModeSetup`() {
        // GIVEN
        val currentStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
    }

    @Test
    fun `given PairYourScanner step, when getting previous step for STAR_BSH_20B, then should return ScannerHIDModeSetup`() {
        // GIVEN
        val currentStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.ScannerHIDModeSetup::class.java)
    }

    @Test
    fun `given PairYourScanner step, when getting previous step for INATECK_BLUETOOTH, then should return ScannerPairModeSetup`() {
        // GIVEN
        val currentStep = ScanningSetupStep.PairYourScanner()

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.INATECK_BLUETOOTH, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.ScannerPairModeSetup::class.java)
    }

    @Test
    fun `given TestYourScanner step, when getting previous step for TERA_1200, then should return PairYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given TestYourScanner step, when getting previous step for STAR_BSH_20B, then should return PairYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given TestYourScanner step, when getting previous step for INATECK_BLUETOOTH, then should return PairYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.TestYourScanner

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.INATECK_BLUETOOTH, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.PairYourScanner::class.java)
    }

    @Test
    fun `given ScannerSetupSuccess step, when getting previous step for TERA_1200, then should return TestYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerSetupSuccess

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.TERA_1200, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given ScannerSetupSuccess step, when getting previous step for STAR_BSH_20B, then should return TestYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerSetupSuccess

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.STAR_BSH_20B, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given ScannerSetupSuccess step, when getting previous step for INATECK_BLUETOOTH, then should return TestYourScanner`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerSetupSuccess

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.INATECK_BLUETOOTH, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.TestYourScanner::class.java)
    }

    @Test
    fun `given ScannerSetupInfo step, when getting previous step for OTHER, then should return DeviceSelection`() {
        // GIVEN
        val currentStep = ScanningSetupStep.ScannerSetupInfo

        // WHEN
        val previousStep = navigator.getPreviousStep(BarcodeReaderDevice.OTHER, currentStep)

        // THEN
        assertThat(previousStep).isInstanceOf(ScanningSetupStep.DeviceSelection::class.java)
    }
}
