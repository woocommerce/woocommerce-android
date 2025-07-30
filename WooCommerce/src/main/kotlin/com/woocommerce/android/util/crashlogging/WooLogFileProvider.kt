package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.WooLog
import java.io.File
import javax.inject.Inject

class WooLogFileProvider @Inject constructor(private val wooLog: WooLog) {
    fun provide(): File {
        return File.createTempFile("log", "").apply {
            appendText(wooLog.provideLogs())
        }
    }
}
