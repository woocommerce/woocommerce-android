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
        return gattScanner ?: findConnectedBondedScanner(bluetoothManager)
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
            type = ScannerType.BLUETOOTH,
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
            val isConnectedViaHid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val hidConnectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.HID_DEVICE)
                hidConnectedDevices.contains(this)
            } else {
                false
            }

            val gattConnectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
            val isConnectedViaGatt = gattConnectedDevices.contains(this)

            isConnectedViaHid || isConnectedViaGatt
        } catch (e: Exception) {
            wooPosLogWrapper.d("Error checking device connection status: ${e.message}")
            false
        }
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
        private const val MIN_EXTERNAL_USB_VENDOR_ID = 0x1000
    }
}

sealed class ScannerInfo {
    data class Connected(
        val name: String,
        val type: ScannerType,
        val deviceClass: Int? = null,
    ) : ScannerInfo()

    data object NoScannerDetected : ScannerInfo()

    data object BluetoothPermissionNotGranted : ScannerInfo()
}

enum class ScannerType {
    BLUETOOTH,
    USB_HID,
}
