package com.woocommerce.android

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.tools.SelectedSite.SelectedSiteResetException
import java.lang.Thread.UncaughtExceptionHandler

/**
 * Handles uncaught exceptions where there are chances to recover from them.
 */
class UncaughtErrorsHandler(
    private val baseHandler: UncaughtExceptionHandler?,
    private val crashLogger: CrashLogging
) : UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        when (e) {
            is SelectedSiteResetException -> {
                // This might thrown on specific cases when the user can't access the site:
                // - The user has been removed from the site
                // - The site doesn't have WooCommerce anymore
                // - During logout flow...

                // Send an error report, the app will be restarted by whatever triggered the reset.
                crashLogger.sendReport(e)
            }
            else -> baseHandler?.uncaughtException(t, e) ?: throw e
        }
    }
}
