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
class WooPosScannerDetectionUtil @Inject constructor(
    private val context: Context,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    fun detectConnectedScanner(context: Context): ScannerInfo? {
        val bluetoothScanner = detectBluetoothScanner()
        if (bluetoothScanner != null) {
            return bluetoothScanner
        }

        return detectUsbHidScanner(context)
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun detectBluetoothScanner(): ScannerInfo? {
        try {
            val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            bluetoothAdapter?.takeIf { it.isEnabled }?.bondedDevices?.forEach { device ->
                if (device.isPotentialBarcodeScanner()) {
                    return createBluetoothScannerInfo(device)
                }
            }
        } catch (e: Exception) {
            wooPosLogWrapper.e("Bluetooth permission not granted. Cannot detect Bluetooth scanners.", e)
        }

        return null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createBluetoothScannerInfo(device: BluetoothDevice): ScannerInfo {
        return ScannerInfo(
            name = device.name ?: "Unknown Bluetooth Scanner",
            type = ScannerType.BLUETOOTH,
        )
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun detectUsbHidScanner(context: Context): ScannerInfo? {
        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
            val inputDeviceIds = inputManager.inputDeviceIds

            inputDeviceIds.forEach { deviceId ->
                val inputDevice = inputManager.getInputDevice(deviceId)
                if (inputDevice != null && inputDevice.isPotentialBarcodeScanner()) {
                    return ScannerInfo(
                        name = inputDevice.name,
                        type = ScannerType.USB_HID,
                    )
                }
            }
        } catch (e: Exception) {
            wooPosLogWrapper.e("Error detecting USB HID scanners: ${e.message}", e)
        }

        return null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun BluetoothDevice.isPotentialBarcodeScanner(): Boolean {
        val deviceClass = bluetoothClass?.deviceClass
        return deviceClass != null && isScannerByDeviceClass(deviceClass)
    }

    private fun InputDevice.isPotentialBarcodeScanner(): Boolean {
        if (!isExternalUsbDevice()) return false

        return sources and InputDevice.SOURCE_KEYBOARD != 0 &&
            keyboardType == InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC
    }

    private fun InputDevice.isExternalUsbDevice(): Boolean {
        val deviceName = name.lowercase()

        val internalDeviceKeywords = listOf(
            "virtual",
            "built-in",
            "internal",
            "qwerty",
            "touchscreen",
            "touch",
            "trackpad",
            "mouse",
            "synaptics",
            "elan",
            "alps"
        )

        return deviceName !in internalDeviceKeywords && vendorId > MIN_EXTERNAL_USB_VENDOR_ID
    }

    private fun isScannerByDeviceClass(deviceClass: Int): Boolean {
        return deviceClass == PERIPHERAL_CLASS ||
            deviceClass == KEYBOARD_CLASS ||
            deviceClass == HID_CLASS
    }

    fun getScannerInfoString(scanner: ScannerInfo?): String {
        if (scanner == null) return "no_scanner_detected"

        return "${scanner.name}-(${scanner.type})"
    }

    private companion object {
        private const val PERIPHERAL_CLASS = 0x500
        private const val KEYBOARD_CLASS = 0x540
        private const val HID_CLASS = 0x580
        private const val MIN_EXTERNAL_USB_VENDOR_ID = 0x1000
    }
}

data class ScannerInfo(
    val name: String,
    val type: ScannerType,
    val deviceClass: Int? = null,
)

enum class ScannerType {
    BLUETOOTH,
    USB_HID,
    UNKNOWN
}
