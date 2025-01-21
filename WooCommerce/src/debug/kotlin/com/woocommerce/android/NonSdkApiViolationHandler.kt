package com.woocommerce.android

import android.os.Build
import android.os.Process
import android.os.StrictMode.OnVmViolationListener
import android.os.strictmode.NonSdkApiUsedViolation
import android.os.strictmode.Violation
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.system.exitProcess

@RequiresApi(Build.VERSION_CODES.P)
@Suppress("MagicNumber")
class NonSdkApiViolationHandler : OnVmViolationListener {
    override fun onVmViolation(v: Violation) {
        if (v !is NonSdkApiUsedViolation) return
        Log.e(
            NonSdkApiViolationHandler::class.simpleName,
            "Non-SDK API violation detected: ${v.message}\n${v.stackTraceToString()}"
        )
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}
