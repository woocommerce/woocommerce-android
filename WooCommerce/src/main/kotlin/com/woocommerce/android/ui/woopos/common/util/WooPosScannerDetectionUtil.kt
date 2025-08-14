package com.woocommerce.android.ui.woopos.common.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice
import androidx.annotation.RequiresPermission
import javax.inject.Inject

class WooPosScannerDetectionUtil @Inject constructor(
    private val context: Context,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    fun detectConnectedScanner(): ScannerInfo {
        val bluetoothScanner = detectBluetoothScanner()
        if (bluetoothScanner !is ScannerInfo.NoScannerDetected) {
            return bluetoothScanner
        }

        val usbScanner = detectUsbHidScanner(context)
        return usbScanner ?: ScannerInfo.NoScannerDetected
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun detectBluetoothScanner(): ScannerInfo {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter?.isEnabled != true) {
                return ScannerInfo.NoScannerDetected
            }

            val connectedScanner = findConnectedScanner(bluetoothManager)
            connectedScanner ?: ScannerInfo.NoScannerDetected
        } catch (e: SecurityException) {
            wooPosLogWrapper.e("Bluetooth permission not granted. Cannot detect Bluetooth scanners.", e)
            ScannerInfo.BluetoothPermissionNotGranted
        } catch (e: Exception) {
            wooPosLogWrapper.e("Error detecting Bluetooth scanners: ${e.message}", e)
            ScannerInfo.NoScannerDetected
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun findConnectedScanner(bluetoothManager: BluetoothManager): ScannerInfo? {
        val hidScanner = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            findScannerInProfile(bluetoothManager, BluetoothProfile.HID_DEVICE)
        } else {
            // For SDK < P, HID_DEVICE profile is not available
            // Fall back to checking bonded devices directly
            null
        }

        val gattScanner = hidScanner ?: findScannerInProfile(bluetoothManager, BluetoothProfile.GATT)
        val gattServerScanner = gattScanner ?: findScannerInProfile(bluetoothManager, BluetoothProfile.GATT_SERVER)

        return gattServerScanner ?: findConnectedBondedScanner(bluetoothManager)
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun findScannerInProfile(bluetoothManager: BluetoothManager, profile: Int): ScannerInfo? {
        return try {
            val connectedDevices = bluetoothManager.getConnectedDevices(profile)
            connectedDevices.firstOrNull { it.isPotentialBarcodeScanner() }
                ?.let { createBluetoothScannerInfo(it) }
        } catch (e: Exception) {
            wooPosLogWrapper.d("Profile $profile not available or no devices connected: ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun findConnectedBondedScanner(bluetoothManager: BluetoothManager): ScannerInfo? {
        return bluetoothManager.adapter?.bondedDevices
            ?.firstOrNull { device ->
                device.isPotentialBarcodeScanner() && device.isConnected(bluetoothManager)
            }?.let { createBluetoothScannerInfo(it) }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun createBluetoothScannerInfo(device: BluetoothDevice): ScannerInfo {
        return ScannerInfo.Connected(
            name = device.name ?: "Unknown Bluetooth Scanner",
            type = ScannerType.BLUETOOTH
        )
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun detectUsbHidScanner(context: Context): ScannerInfo.Connected? {
        try {
            val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
            val inputDeviceIds = inputManager.inputDeviceIds

            inputDeviceIds.forEach { deviceId ->
                val inputDevice = inputManager.getInputDevice(deviceId)
                if (inputDevice != null && inputDevice.isPotentialBarcodeScanner()) {
                    return ScannerInfo.Connected(
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

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun BluetoothDevice.isConnected(bluetoothManager: BluetoothManager): Boolean {
        return try {
            val method = this.javaClass.getMethod("isConnected")
            method.invoke(this) as Boolean
        } catch (e: Exception) {
            wooPosLogWrapper.d("Failed to check connection via reflection: ${e.message}")
            isConnectedViaProfiles(bluetoothManager)
        }
    }

    @SuppressLint("MissingPermission")
    @Suppress("TooGenericExceptionCaught")
    private fun BluetoothDevice.isConnectedViaProfiles(bluetoothManager: BluetoothManager): Boolean {
        val profiles = listOf(
            BluetoothProfile.GATT,
            BluetoothProfile.GATT_SERVER
        ).plus(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                listOf(BluetoothProfile.HID_DEVICE)
            } else {
                emptyList()
            }
        )

        return profiles.any { profile ->
            try {
                val connectedDevices = bluetoothManager.getConnectedDevices(profile)
                connectedDevices.contains(this)
            } catch (e: Exception) {
                wooPosLogWrapper.d("Profile $profile not supported: ${e.message}")
                false
            }
        }
    }

    private fun InputDevice.isPotentialBarcodeScanner(): Boolean {
        if (!isExternalUsbDevice()) return false

        return sources and InputDevice.SOURCE_KEYBOARD != 0 &&
            keyboardType == InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC
    }

    private fun InputDevice.isExternalUsbDevice(): Boolean {
        val deviceName = name.lowercase()

        val scannerNamePatterns = listOf(
            "scanner", "barcode", "symbol", "zebra", "honeywell", "datalogic",
            "code", "socket", "unitech", "newland", "inateck", "tera"
        )
        val hasScannerName = scannerNamePatterns.any { deviceName.contains(it) }

        val internalDeviceKeywords = listOf(
            "virtual", "built-in", "internal", "qwerty", "touchscreen", "touch",
            "trackpad", "mouse", "synaptics", "elan", "alps", "uinput", "fpc",
            "fingerprint", "sensor", "camera", "mic", "audio", "volume", "power",
            "gpio", "i2c", "spi", "uart", "pwm", "adc", "platform", "soc"
        )
        val isInternalDevice = internalDeviceKeywords.any { deviceName.contains(it) }

        return !isInternalDevice &&
            vendorId > MIN_EXTERNAL_USB_VENDOR_ID &&
            (hasScannerName || isKnownScannerVendor())
    }

    private fun InputDevice.isKnownScannerVendor(): Boolean {
        // Common barcode scanner vendor IDs (hexadecimal)
        @Suppress("MagicNumber")
        val knownScannerVendorIds = setOf(
            0x05e0, // Symbol Technologies (now Zebra)
            0x0536, // Hand Held Products (Honeywell)
            0x1504, // Microscan
            0x0c2e, // Metrologic (now Honeywell)
            0x04b4, // Cypress (some scanner chips)
            0x05f9, // PSC (now Honeywell)
            0x1900, // Socket Mobile
            0x0461, // Primax
            0x04d9, // Holtek (some scanner controllers)
            0x1a86, // QinHeng Electronics (some scanner chips)
        )
        return vendorId in knownScannerVendorIds
    }

    private fun isScannerByDeviceClass(deviceClass: Int): Boolean {
        return deviceClass == PERIPHERAL_CLASS ||
            deviceClass == KEYBOARD_CLASS ||
            deviceClass == HID_CLASS
    }

    fun getScannerInfoString(scanner: ScannerInfo): String {
        return when (scanner) {
            is ScannerInfo.Connected -> "${scanner.name}-(${scanner.type})"
            is ScannerInfo.NoScannerDetected -> "no_scanner_detected"
            is ScannerInfo.BluetoothPermissionNotGranted -> "bluetooth_permission_not_granted"
        }
    }

    private companion object {
        private const val PERIPHERAL_CLASS = 0x500
        private const val KEYBOARD_CLASS = 0x540
        private const val HID_CLASS = 0x580
        private const val MIN_EXTERNAL_USB_VENDOR_ID = 0x100
    }
}

sealed class ScannerInfo {
    data class Connected(
        val name: String,
        val type: ScannerType,
    ) : ScannerInfo()

    data object NoScannerDetected : ScannerInfo()

    data object BluetoothPermissionNotGranted : ScannerInfo()
}

enum class ScannerType {
    BLUETOOTH,
    USB_HID,
}
