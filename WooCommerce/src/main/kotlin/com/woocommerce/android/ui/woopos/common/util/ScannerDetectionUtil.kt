package com.woocommerce.android.ui.woopos.common.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import androidx.annotation.RequiresPermission
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScannerDetectionUtil @Inject constructor(
    private val context: Context,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {

    fun detectConnectedScanners(context: Context): List<ScannerInfo> {
        val scanners = mutableListOf<ScannerInfo>()

        scanners.addAll(detectBluetoothScanners())
        scanners.addAll(detectUsbHidScanners(context))

        return scanners
    }

    @SuppressLint("MissingPermission")
    private fun detectBluetoothScanners(): List<ScannerInfo> {
        val scanners = mutableListOf<ScannerInfo>()

        try {
            val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            bluetoothAdapter?.takeIf { it.isEnabled }?.bondedDevices?.forEach { device ->
                if (isPotentialBarcodeScanner(device)) {
                    scanners.add(createBluetoothScannerInfo(device))
                }
            }
        } catch (e: Exception) {
            wooPosLogWrapper.e("Bluetooth permission not granted. Cannot detect Bluetooth scanners.", e)
        }

        return scanners
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createBluetoothScannerInfo(device: BluetoothDevice): ScannerInfo {
        return ScannerInfo(
            name = device.name ?: "Unknown Bluetooth Scanner",
            type = ScannerType.BLUETOOTH,
            deviceClass = device.bluetoothClass?.deviceClass,
        )
    }

    private fun detectUsbHidScanners(context: Context): List<ScannerInfo> {
        val scanners = mutableListOf<ScannerInfo>()

        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
            val inputDeviceIds = inputManager.inputDeviceIds

            inputDeviceIds.forEach { deviceId ->
                val inputDevice = inputManager.getInputDevice(deviceId)
                if (inputDevice != null && isPotentialBarcodeScanner(inputDevice)) {
                    scanners.add(
                        ScannerInfo(
                            name = inputDevice.name,
                            type = ScannerType.USB_HID,
                            vendorId = inputDevice.vendorId,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions during device detection - this is expected
        }

        return scanners
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun isPotentialBarcodeScanner(device: BluetoothDevice): Boolean {
        val deviceName = device.name?.lowercase() ?: return false
        return isScannerByName(deviceName)
    }

    @Suppress("ComplexCondition")
    private fun isPotentialBarcodeScanner(inputDevice: InputDevice): Boolean {
        val deviceName = inputDevice.name.lowercase()

        if (isScannerByName(deviceName)) {
            return true
        }

        return inputDevice.sources and InputDevice.SOURCE_KEYBOARD != 0 &&
            inputDevice.keyboardType == InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC
    }

    private fun isScannerByName(deviceName: String): Boolean {
        return deviceName.contains("scanner", ignoreCase = true) ||
            deviceName.contains("zebra", ignoreCase = true) ||
            deviceName.contains("honeywell", ignoreCase = true) ||
            deviceName.contains("symbol", ignoreCase = true) ||
            deviceName.contains("datalogic", ignoreCase = true)
    }

    fun getScannerInfoString(scanners: List<ScannerInfo>): String {
        if (scanners.isEmpty()) return "no_scanner_detected"

        return scanners.joinToString(separator = ";") { scanner ->
            "${scanner.name}-(${scanner.type})-${scanner.deviceClass ?: "N/A"}-${scanner.vendorId ?: "N/A"}"
        }
    }
}

data class ScannerInfo(
    val name: String,
    val type: ScannerType,
    val deviceClass: Int? = null,
    val vendorId: Int? = null,
)

enum class ScannerType {
    BLUETOOTH,
    USB_HID,
    UNKNOWN
}
