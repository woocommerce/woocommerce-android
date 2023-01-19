package com.woocommerce.android.extensions

import android.app.ActivityManager
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.nfc.NfcAdapter
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.woocommerce.android.util.SystemVersionUtils

fun Context.getColorCompat(@ColorRes colorRes: Int) = ContextCompat.getColor(this, colorRes)

fun Context.copyToClipboard(label: String, text: String) {
    with(getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager) {
        setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

fun Context.getCurrentProcessName() =
    if (SystemVersionUtils.isAtLeastP()) {
        Application.getProcessName()
    } else {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        am.runningAppProcesses.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
    }

fun Context.isGooglePlayServicesAvailable(): Boolean =
    GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS

fun Context.isDeviceWithNFC(): Boolean =
    NfcAdapter.getDefaultAdapter(this) != null
